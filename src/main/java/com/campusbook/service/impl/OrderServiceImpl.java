package com.campusbook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.campusbook.common.BusinessException;
import com.campusbook.common.PageResult;
import com.campusbook.common.ResultCode;
import com.campusbook.context.UserContext;
import com.campusbook.dto.OrderCreateRequest;
import com.campusbook.entity.Book;
import com.campusbook.entity.Order;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.OrderMapper;
import com.campusbook.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 订单状态常量，避免魔法数字散落 */
    private static final int STATUS_WAIT_PAY = 0;
    private static final int STATUS_WAIT_HANDOVER = 1;
    private static final int STATUS_FINISHED = 2;
    private static final int STATUS_CANCELED = 3;
    private static final int STATUS_REFUNDED = 4;

    private final BookMapper bookMapper;

    /** 待支付订单的超时时间（分钟），默认 30 */
    @Value("${app.order.timeout-minutes:30}")
    private long timeoutMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrderCreateRequest request) {
        Long buyerId = UserContext.requireUserId();

        Book book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "书籍不存在或已下架");
        }
        if (!Integer.valueOf(1).equals(book.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "这本书已下架、已预订或已售出");
        }
        if (Objects.equals(book.getSellerId(), buyerId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能购买自己发布的书籍");
        }

        /*
         * 核心防超卖：
         * 一条 UPDATE 同时完成"校验在售 + 扣库存 + 售罄置为已售出"，依赖 InnoDB 行锁串行化。
         * 不能先 select stock 再 update，那样两个请求会同时读到 stock=1 然后各扣一次。
         */
        int affected = bookMapper.decreaseStock(book.getId());
        if (affected == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "手慢了，该书已被抢走");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBookId(book.getId());
        order.setBuyerId(buyerId);
        order.setSellerId(book.getSellerId());
        order.setBookTitle(book.getTitle());
        order.setUnitPrice(book.getPrice());
        order.setQuantity(1);
        order.setTotalAmount(book.getPrice());
        order.setStatus(STATUS_WAIT_PAY);
        order.setDeliveryType(request.getDeliveryType() == null ? 1 : request.getDeliveryType());
        order.setDeliveryAddr(request.getDeliveryAddr());
        order.setRemark(request.getRemark());

        save(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        Long userId = UserContext.requireUserId();
        Order order = getOrderOrThrow(orderId);

        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有买家可以支付订单");
        }
        if (!Integer.valueOf(STATUS_WAIT_PAY).equals(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "订单当前状态不可支付");
        }

        // 状态作为 WHERE 条件，重复支付或并发支付时只有一个能成功
        boolean updated = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getBuyerId, userId)
                .eq(Order::getStatus, STATUS_WAIT_PAY)
                .set(Order::getStatus, STATUS_WAIT_HANDOVER)
                .set(Order::getPayTime, LocalDateTime.now())
                .update();

        if (!updated) {
            throw new BusinessException(ResultCode.CONFLICT, "订单状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        Long userId = UserContext.requireUserId();
        Order order = getOrderOrThrow(orderId);

        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有买家可以确认收货");
        }
        if (!Integer.valueOf(STATUS_WAIT_HANDOVER).equals(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "订单当前状态不可确认收货");
        }

        boolean updated = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getBuyerId, userId)
                .eq(Order::getStatus, STATUS_WAIT_HANDOVER)
                .set(Order::getStatus, STATUS_FINISHED)
                .set(Order::getFinishTime, LocalDateTime.now())
                .update();

        if (!updated) {
            throw new BusinessException(ResultCode.CONFLICT, "订单状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        Long userId = UserContext.requireUserId();
        Order order = getOrderOrThrow(orderId);

        boolean isBuyer = Objects.equals(order.getBuyerId(), userId);
        boolean isSeller = Objects.equals(order.getSellerId(), userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权取消该订单");
        }

        Integer current = order.getStatus();
        if (!Integer.valueOf(STATUS_WAIT_PAY).equals(current)
                && !Integer.valueOf(STATUS_WAIT_HANDOVER).equals(current)) {
            throw new BusinessException(ResultCode.CONFLICT, "订单当前状态不可取消");
        }

        // 未支付取消是"已取消"，已支付取消是"已退款"
        int target = Integer.valueOf(STATUS_WAIT_PAY).equals(current) ? STATUS_CANCELED : STATUS_REFUNDED;

        boolean updated = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, current)
                .set(Order::getStatus, target)
                .set(Order::getCancelReason, isBuyer ? "买家取消" : "卖家取消")
                .update();

        if (!updated) {
            throw new BusinessException(ResultCode.CONFLICT, "订单状态已变化，请刷新后重试");
        }

        // 只有订单状态真正改成功才回滚库存，避免重复 +1
        bookMapper.restoreStock(order.getBookId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // 一次最多处理 100 笔，避免积压太多时长时间占事务
        List<Order> expired = lambdaQuery()
                .eq(Order::getStatus, STATUS_WAIT_PAY)
                .lt(Order::getCreatedAt, deadline)
                .last("LIMIT 100")
                .list();

        int count = 0;
        for (Order order : expired) {
            // 条件更新：只有仍处于待支付才取消，防止和用户手动支付/取消并发
            boolean updated = lambdaUpdate()
                    .eq(Order::getId, order.getId())
                    .eq(Order::getStatus, STATUS_WAIT_PAY)
                    .set(Order::getStatus, STATUS_CANCELED)
                    .set(Order::getCancelReason, "超时未支付，系统自动取消")
                    .update();

            if (updated) {
                // 订单状态改成功才回滚库存，保证只回滚一次
                bookMapper.restoreStock(order.getBookId());
                count++;
            }
        }
        return count;
    }

    @Override
    public Order detail(Long orderId) {
        Long userId = UserContext.requireUserId();
        Order order = getOrderOrThrow(orderId);

        if (!Objects.equals(order.getBuyerId(), userId) && !Objects.equals(order.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该订单");
        }
        return order;
    }

    @Override
    public PageResult<Order> listMine(String role, Integer status, Integer pageNum, Integer pageSize) {
        Long userId = UserContext.requireUserId();

        boolean isSeller = "seller".equalsIgnoreCase(role);
        boolean isBuyer = "buyer".equalsIgnoreCase(role);
        if (!isSeller && !isBuyer) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "role 只能是 buyer 或 seller");
        }

        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);

        Page<Order> p = new Page<>(page, size);

        LambdaQueryWrapper<Order> wrapper = Wrappers.lambdaQuery();
        if (isSeller) {
            wrapper.eq(Order::getSellerId, userId);
        } else {
            wrapper.eq(Order::getBuyerId, userId);
        }
        wrapper.eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt)
                .orderByDesc(Order::getId);

        page(p, wrapper);

        return PageResult.of(
                p.getTotal(),
                (int) p.getCurrent(),
                (int) p.getSize(),
                p.getRecords()
        );
    }

    private Order getOrderOrThrow(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /**
     * 订单号：14 位时间 + 6 位随机数。
     * 高并发下仍可能撞唯一索引，生产建议雪花算法或独立发号器。
     */
    private String generateOrderNo() {
        return ORDER_NO_FORMAT.format(LocalDateTime.now())
                + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }
}
