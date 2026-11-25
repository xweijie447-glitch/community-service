package com.community.domain;

import com.community.storage.HasId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 订单状态流水，用于追溯与幂等审计
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderStatusLog implements HasId {

    private Long id;

    private Long orderId;

    private String orderNo;

    private OrderStatus fromStatus;

    private OrderStatus toStatus;

    private String operator;

    private String remark;

    private LocalDateTime createdAt;

    public static OrderStatusLog of(ServiceOrder order, OrderStatus from, OrderStatus to,
                                    String operator, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(operator);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
