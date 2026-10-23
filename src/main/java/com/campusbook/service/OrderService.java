package com.campusbook.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.campusbook.common.PageResult;
import com.campusbook.dto.OrderCreateRequest;
import com.campusbook.entity.Order;

/**
 * 订单业务：下单、支付、确认收货、取消、超时处理、详情、我的订单。
 */
public interface OrderService extends IService<Order> {

    /**
     * 买家下单，返回订单 ID。
     *
     * <p>buyerId 由实现类从 UserContext 取，Controller 不传，避免前端伪造买家。</p>
     */
    Long create(OrderCreateRequest request);

    /**
     * 买家支付：0 待支付 -> 1 待交接。
     */
    void pay(Long orderId);

    /**
     * 买家确认收货：1 待交接 -> 2 已完成。
     */
    void confirm(Long orderId);

    /**
     * 取消订单：0 待支付 -> 3 已取消，1 待交接 -> 4 已退款，并回滚书籍库存。
     * 买卖双方都可以取消。
     */
    void cancel(Long orderId);

    /**
     * 系统任务：取消超时未支付的订单并回滚库存，返回处理数量。
     */
    int cancelTimeoutOrders();

    /**
     * 订单详情，仅买卖双方可看。
     */
    Order detail(Long orderId);

    /**
     * 分页查询我的订单。
     *
     * @param role     buyer-我买到的，seller-我卖出的
     * @param status   可选，按状态过滤
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     */
    PageResult<Order> listMine(String role, Integer status, Integer pageNum, Integer pageSize);
}
