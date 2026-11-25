package com.community.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：
 * <pre>
 * community.order.exchange (direct)
 *   ├── order.delay.queue    rk=order.delay    TTL 到期后死信转发 -> order.timeout
 *   ├── order.timeout.queue  rk=order.timeout  超时自动取消订单
 *   ├── order.notify.queue   rk=order.notify   派单 / 完成通知异步发送
 *   └── review.stats.queue   rk=review.stats   评价异步统计
 * </pre>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    public static final String ORDER_EXCHANGE = "community.order.exchange";

    public static final String RK_DELAY = "order.delay";
    public static final String RK_TIMEOUT = "order.timeout";
    public static final String RK_NOTIFY = "order.notify";
    public static final String RK_REVIEW = "review.stats";

    public static final String QUEUE_DELAY = "order.delay.queue";
    public static final String QUEUE_TIMEOUT = "order.timeout.queue";
    public static final String QUEUE_NOTIFY = "order.notify.queue";
    public static final String QUEUE_REVIEW = "review.stats.queue";

    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /** 延迟队列：队列级 TTL 统一控制超时时长，到期后死信到超时队列 */
    @Bean
    public Queue orderDelayQueue() {
        long ttl = Math.max(1, appProperties.getOrder().getTimeoutMinutes()) * 60_000L;
        return QueueBuilder.durable(QUEUE_DELAY)
                .ttl((int) Math.min(ttl, Integer.MAX_VALUE))
                .deadLetterExchange(ORDER_EXCHANGE)
                .deadLetterRoutingKey(RK_TIMEOUT)
                .build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(QUEUE_TIMEOUT).build();
    }

    @Bean
    public Queue orderNotifyQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFY).build();
    }

    @Bean
    public Queue reviewStatsQueue() {
        return QueueBuilder.durable(QUEUE_REVIEW).build();
    }

    @Bean
    public Binding bindDelay(Queue orderDelayQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderExchange).with(RK_DELAY);
    }

    @Bean
    public Binding bindTimeout(Queue orderTimeoutQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderExchange).with(RK_TIMEOUT);
    }

    @Bean
    public Binding bindNotify(Queue orderNotifyQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderNotifyQueue).to(orderExchange).with(RK_NOTIFY);
    }

    @Bean
    public Binding bindReview(Queue reviewStatsQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(reviewStatsQueue).to(orderExchange).with(RK_REVIEW);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息投递失败 correlation={} cause={}", correlationData, cause);
            }
        });
        template.setReturnsCallback(returned ->
                log.error("消息路由失败 exchange={} routingKey={} reply={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }
}
