package com.campusbook.controller;

import com.campusbook.common.PageResult;
import com.campusbook.common.Result;
import com.campusbook.dto.OrderCreateRequest;
import com.campusbook.entity.Order;
import com.campusbook.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口，全部需要登录，当前用户从 UserContext 取。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 买家下单，请求体只需要 bookId。
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.create(request));
    }

    /**
     * 买家支付：待支付 -> 待交接。
     */
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success();
    }

    /**
     * 买家确认收货：待交接 -> 已完成。
     */
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return Result.success();
    }

    /**
     * 取消订单：待支付 -> 已取消，待交接 -> 已退款，并回滚库存。
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /**
     * 我的订单，分页。
     *
     * @param role     buyer-我买到的（默认），seller-我卖出的
     * @param status   可选，按订单状态过滤
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10，最大 100
     */
    @GetMapping
    public Result<PageResult<Order>> listMine(@RequestParam(defaultValue = "buyer") String role,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.listMine(role, status, pageNum, pageSize));
    }
}
