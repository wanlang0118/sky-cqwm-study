package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单状态定时处理
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 每分钟检查一次：下单超过15分钟未支付则自动取消
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        List<Orders> list = ordersMapper.listByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, deadline);
        if (list == null || list.isEmpty()) {
            return;
        }
        log.info("取消超时未支付订单数量：{}", list.size());
        for (Orders orders : list) {
            Orders toUpdate = Orders.builder()
                    .id(orders.getId())
                    .status(Orders.CANCELLED)
                    .payStatus(Orders.UN_PAID)
                    .cancelTime(LocalDateTime.now())
                    .cancelReason("支付超时，系统自动取消")
                    .build();
            ordersMapper.update(toUpdate);
        }
    }

    /**
     * 每天凌晨1点：派送中订单自动完成
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void completeDeliveringOrders() {
        List<Orders> list = ordersMapper.listByStatus(Orders.DELIVERY_IN_PROGRESS);
        if (list == null || list.isEmpty()) {
            return;
        }
        log.info("自动完成派送中订单数量：{}", list.size());
        for (Orders orders : list) {
            Orders toUpdate = Orders.builder()
                    .id(orders.getId())
                    .status(Orders.COMPLETED)
                    .deliveryTime(orders.getDeliveryTime() == null ? LocalDateTime.now() : orders.getDeliveryTime())
                    .build();
            ordersMapper.update(toUpdate);
        }
    }
}
