package com.community.mq;

import com.community.config.RabbitConfig;
import com.community.mq.message.NotifyMessage;
import com.community.mq.message.OrderTimeoutMessage;
import com.community.mq.message.ReviewStatsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /** 投递超时检查：延迟队列按队列级 TTL 到期后死信到超时队列 */
    public void sendTimeoutCheck(Long orderId, String orderNo, int timeoutMinutes) {
        OrderTimeoutMessage message = new OrderTimeoutMessage(orderId, orderNo, System.currentTimeMillis());
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.RK_DELAY, message, msg -> {
                msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                msg.getMessageProperties().setMessageId("timeout-" + orderNo);
                return msg;
            });
            log.info("投递超时检查消息 orderNo={} 超时={}min（队列 TTL 生效）", orderNo, timeoutMinutes);
        } catch (Exception e) {
            // MQ 不可用不影响下单，超时兜底由定时补偿扫描完成
            log.error("投递超时检查消息失败 orderNo={}，将由补偿扫描兜底", orderNo, e);
        }
    }

    /** 异步通知（邮件 / 短信） */
    public void sendNotify(Long orderId, String orderNo, String type, String extra) {
        NotifyMessage message = new NotifyMessage(orderId, orderNo, type, extra);
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.RK_NOTIFY, message);
            log.info("投递通知消息 orderNo={} type={}", orderNo, type);
        } catch (Exception e) {
            log.error("投递通知消息失败 orderNo={} type={}", orderNo, type, e);
        }
    }

    /** 评价异步统计 */
    public void sendReviewStats(ReviewStatsMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.RK_REVIEW, message);
            log.info("投递评价统计消息 orderNo={} score={} at={}", message.getOrderNo(), message.getScore(),
                    LocalDateTime.now());
        } catch (Exception e) {
            log.error("投递评价统计消息失败 orderNo={}", message.getOrderNo(), e);
        }
    }
}
