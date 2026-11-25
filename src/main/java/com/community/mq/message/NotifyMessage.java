package com.community.mq.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 通知消息：订单创建 / 派单 / 完成 / 超时取消
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String DISPATCHED = "DISPATCHED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private Long orderId;
    private String orderNo;
    private String type;
    private String extra;
}
