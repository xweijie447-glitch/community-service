package com.community.mq.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 订单超时检查消息，投递到延迟队列，TTL 到期后死信进入超时处理队列
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage {

    private Long orderId;
    private String orderNo;
    private long createdAt;
}
