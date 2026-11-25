package com.community.mq;

import com.community.config.RabbitConfig;
import com.community.mq.message.NotifyMessage;
import com.community.mq.message.OrderTimeoutMessage;
import com.community.mq.message.ReviewStatsMessage;
import com.community.service.NotificationService;
import com.community.service.OrderService;
import com.community.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单相关消息消费者
 * <p>消费端统一做幂等：消息携带业务唯一键，Redis 记录已处理标记，重复投递直接丢弃。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMQConsumer {

    private static final String DEDUPE_PREFIX = "mq:dedupe:";

    private final OrderService orderService;
    private final NotificationService notificationService;
    private final ReviewService reviewService;
    private final StringRedisTemplate redisTemplate;

    /** 超时未派单自动取消 */
    @RabbitListener(queues = RabbitConfig.QUEUE_TIMEOUT)
    public void onOrderTimeout(OrderTimeoutMessage message) {
        if (message == null || message.getOrderId() == null) {
            return;
        }
        log.info("收到超时检查消息 orderNo={}", message.getOrderNo());
        orderService.cancelByTimeout(message.getOrderId());
    }

    /** 派单 / 完成 / 取消 通知异步发送 */
    @RabbitListener(queues = RabbitConfig.QUEUE_NOTIFY)
    public void onNotify(NotifyMessage message) {
        if (message == null || message.getOrderId() == null) {
            return;
        }
        String dedupeKey = DEDUPE_PREFIX + "notify:" + message.getOrderNo() + ":" + message.getType();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofDays(1));
        if (Boolean.FALSE.equals(first)) {
            log.info("重复通知消息已忽略 orderNo={} type={}", message.getOrderNo(), message.getType());
            return;
        }
        switch (message.getType()) {
            case NotifyMessage.ORDER_CREATED -> notificationService.orderCreated(message.getOrderId());
            case NotifyMessage.DISPATCHED -> notificationService.dispatched(message.getOrderId());
            case NotifyMessage.COMPLETED -> notificationService.completed(message.getOrderId());
            case NotifyMessage.CANCELLED -> notificationService.cancelled(message.getOrderId(), message.getExtra());
            default -> log.warn("未知通知类型 {}", message.getType());
        }
    }

    /** 评价异步统计 */
    @RabbitListener(queues = RabbitConfig.QUEUE_REVIEW)
    public void onReviewStats(ReviewStatsMessage message) {
        if (message == null || message.getReviewId() == null) {
            return;
        }
        String dedupeKey = DEDUPE_PREFIX + "review:" + message.getReviewId();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofDays(7));
        if (Boolean.FALSE.equals(first)) {
            log.info("重复评价统计消息已忽略 reviewId={}", message.getReviewId());
            return;
        }
        reviewService.applyStats(message);
    }
}
