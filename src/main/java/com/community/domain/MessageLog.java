package com.community.domain;

import com.community.storage.HasId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 邮件 / 短信发送记录（模拟通道同样落库，便于演示与审计）
 */
@Getter
@Setter
@NoArgsConstructor
public class MessageLog implements HasId {

    private Long id;

    /** 业务类型：ORDER_CREATED / DISPATCHED / COMPLETED / CANCELLED / DAILY_REMIND */
    private String bizType;

    /** 通道：EMAIL / SMS */
    private String channel;

    private String recipient;

    private String subject;

    private String content;

    /** SIMULATED / SENT / FAILED */
    private String status;

    private String orderNo;

    private LocalDateTime createdAt;
}
