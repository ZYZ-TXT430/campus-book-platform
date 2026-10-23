package com.campusbook.task;

import com.campusbook.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时任务。
 *
 * <p>每分钟扫描一次待支付订单，超过 app.order.timeout-minutes（默认 30 分钟）
 * 仍未支付的，自动取消并回滚库存。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    /**
     * fixedDelay：上一次执行完再等 60 秒，不会因为任务变慢而堆积。
     * initialDelay：应用启动 30 秒后开始第一次扫描，避开启动高峰。
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelTimeoutOrders() {
        int count = orderService.cancelTimeoutOrders();
        if (count > 0) {
            log.info("自动取消超时未支付订单 {} 笔", count);
        }
    }
}
