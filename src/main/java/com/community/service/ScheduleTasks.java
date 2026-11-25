package com.community.service;

import com.community.config.AppProperties;
import com.community.domain.OrderStatus;
import com.community.domain.ServiceOrder;
import com.community.repository.ServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务
 * <ol>
 *   <li>清理超过保留天数的已完成订单：标记归档，列表默认不再展示（数据保留可审计）；</li>
 *   <li>每日生成次日待服务工单提醒，推送邮件 / 短信给服务人员与管理员。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTasks {

    private final ServiceOrderRepository orderRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    /** 每日 02:30 归档超期已完成订单 */
    @Scheduled(cron = "${app.schedule.cleanup-cron}")
    public void cleanupCompletedOrders() {
        AppProperties.Schedule cfg = appProperties.getSchedule();
        if (!cfg.isEnabled()) {
            return;
        }
        int keepDays = appProperties.getOrder().getKeepDays();
        LocalDateTime limit = LocalDateTime.now().minusDays(keepDays);
        List<ServiceOrder> expired = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.COMPLETED, limit)
                .stream()
                .filter(o -> o.getCompletedAt() != null && o.getCompletedAt().isBefore(limit))
                .filter(o -> !Boolean.TRUE.equals(o.getArchived()))
                .toList();
        if (expired.isEmpty()) {
            log.info("历史订单清理：没有超过 {} 天的已完成订单", keepDays);
            return;
        }
        expired.forEach(o -> o.setArchived(true));
        orderRepository.saveAll(expired);
        log.info("历史订单清理完成：归档 {} 单（保留天数 {}）", expired.size(), keepDays);
    }

    /** 每日 08:00 推送次日待服务工单提醒 */
    @Scheduled(cron = "${app.schedule.daily-remind-cron}")
    public void dailyWorkOrderRemind() {
        if (!appProperties.getSchedule().isEnabled()) {
            return;
        }
        List<ServiceOrder> tomorrow = orderService.findTomorrowAssigned();
        log.info("次日待服务工单 {} 单，开始推送提醒", tomorrow.size());
        notificationService.dailyRemind(tomorrow);
    }

    /**
     * 每 5 分钟补偿扫描：MQ 不可用或消息丢失时兜底取消超时未派单的订单。
     * <p>取消逻辑本身幂等（只有待接单状态才会被执行），与方法一互不影响。</p>
     */
    @Scheduled(fixedDelay = 300_000L, initialDelay = 60_000L)
    public void compensateTimeoutOrders() {
        if (!appProperties.getSchedule().isEnabled()) {
            return;
        }
        int timeoutMinutes = appProperties.getOrder().getTimeoutMinutes();
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ServiceOrder> stale = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, deadline);
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(order -> orderService.cancelByTimeout(order.getId()));
        log.info("补偿扫描：超时未派单订单 {} 单已自动取消", stale.size());
    }
}
