package com.community.service;

import com.community.common.BusinessException;
import com.community.config.AppProperties;
import com.community.domain.OrderStatus;
import com.community.domain.ServiceItem;
import com.community.domain.ServiceOrder;
import com.community.domain.StaffProfile;
import com.community.domain.StaffStatus;
import com.community.domain.User;
import com.community.repository.ServiceItemRepository;
import com.community.repository.ServiceOrderRepository;
import com.community.repository.StaffProfileRepository;
import com.community.repository.UserRepository;
import com.community.vo.DispatchCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 智能派单：按技能匹配度、评分、当前负载、可服务时间综合打分，给出候选排名。
 * <p>自动派单取榜首，人工可在同一份候选列表上改派，兼顾效率与可控性。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final List<OrderStatus> ACTIVE_STATUS = List.of(OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE);

    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final ServiceItemRepository itemRepository;
    private final ServiceOrderRepository orderRepository;
    private final OrderService orderService;
    private final AppProperties appProperties;

    /**
     * 计算派单候选排名
     */
    public List<DispatchCandidate> recommend(ServiceOrder order) {
        ServiceItem item = itemRepository.findById(order.getServiceItemId()).orElse(null);
        int duration = item == null || item.getDurationMinutes() == null ? 60 : item.getDurationMinutes();
        LocalDateTime start = order.getAppointmentTime();
        LocalDateTime end = start.plusMinutes(duration);

        AppProperties.Dispatch cfg = appProperties.getDispatch();
        double weightSum = cfg.getSkillWeight() + cfg.getRatingWeight() + cfg.getLoadWeight();

        List<DispatchCandidate> candidates = new ArrayList<>();
        for (StaffProfile profile : staffProfileRepository.findAll()) {
            User user = userRepository.findById(profile.getUserId()).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                continue;
            }
            long active = orderRepository.countByStaffIdAndStatusIn(profile.getUserId(), ACTIVE_STATUS);
            boolean capacity = active < profile.getMaxDailyOrders();
            boolean inWorkTime = !start.toLocalTime().isBefore(profile.getWorkStart())
                    && !end.toLocalTime().isAfter(profile.getWorkEnd());
            boolean noConflict = !hasTimeConflict(profile.getUserId(), start, end, order.getId());
            boolean onDuty = profile.getStatus() != StaffStatus.OFF;
            boolean available = capacity && inWorkTime && noConflict && onDuty;

            List<String> matched = matchSkills(profile, order.getServiceItemName(), order.getCategoryName());
            double skillScore = matched.isEmpty() ? 0 : 100;

            // 技能未命中但分类一致，给基础分，避免完全派不出去
            if (matched.isEmpty() && order.getCategoryName() != null
                    && profile.getSkillList().stream().anyMatch(s -> s.length() > 1
                    && order.getCategoryName().contains(s.substring(0, 2)))) {
                skillScore = 55;
            }

            double ratingScore = Math.min(100, profile.getAvgScore() / 5.0 * 100);
            double loadScore = profile.getMaxDailyOrders() == 0 ? 0
                    : Math.max(0, profile.getMaxDailyOrders() - active) * 100.0 / profile.getMaxDailyOrders();

            double total = (skillScore * cfg.getSkillWeight()
                    + ratingScore * cfg.getRatingWeight()
                    + loadScore * cfg.getLoadWeight()) / weightSum;
            if (!available) {
                total = total * 0.3;
            }

            String reason = buildReason(available, capacity, inWorkTime, noConflict, onDuty, matched);
            candidates.add(new DispatchCandidate(profile.getUserId(), user.getDisplayName(),
                    Math.round(total * 10) / 10.0, skillScore, Math.round(ratingScore * 10) / 10.0,
                    Math.round(loadScore * 10) / 10.0, matched, active,
                    Math.max(0, profile.getMaxDailyOrders() - (int) active), profile.getStatus().getLabel(),
                    available, reason));
        }
        candidates.sort(Comparator.comparingDouble(DispatchCandidate::getTotalScore).reversed());
        return candidates;
    }

    /**
     * 自动派单：优先技能命中且在岗有余量的人，无人命中则退化为综合最优
     */
    public ServiceOrder autoAssign(Long orderId) {
        ServiceOrder order = orderService.getOrder(orderId);
        if (order.getStatus().isFinal()) {
            throw BusinessException.stateConflict("订单已结束，无法派单");
        }
        List<DispatchCandidate> candidates = recommend(order);
        DispatchCandidate best = candidates.stream()
                .filter(c -> c.isAvailable() && c.getSkillScore() > 0)
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(DispatchCandidate::isAvailable)
                        .findFirst()
                        .orElse(null));
        if (best == null) {
            throw new BusinessException("当前没有可接单的服务人员，请调整人员状态或稍后再试");
        }
        String reason = best.getMatchedSkills().isEmpty()
                ? "无技能完全匹配，按评分与负载择优"
                : "技能匹配：" + String.join("、", best.getMatchedSkills());
        log.info("自动派单 orderNo={} -> {} 得分 {}", order.getOrderNo(), best.getStaffName(), best.getScoreText());
        return orderService.assign(orderId, best.getStaffId(), "AUTO", best.getTotalScore(), reason);
    }

    /** 人工派单 / 改派 */
    public ServiceOrder manualAssign(Long orderId, Long staffUserId, String remark) {
        return orderService.assign(orderId, staffUserId, "MANUAL", 0.0,
                remark == null || remark.isBlank() ? "管理员手动指派" : remark);
    }

    private boolean hasTimeConflict(Long staffUserId, LocalDateTime start, LocalDateTime end, Long excludeOrderId) {
        List<ServiceOrder> orders = orderRepository.findByStaffIdAndStatusInOrderByAppointmentTimeAsc(
                staffUserId, ACTIVE_STATUS);
        for (ServiceOrder o : orders) {
            if (Objects.equals(o.getId(), excludeOrderId)) {
                continue;
            }
            ServiceItem item = itemRepository.findById(o.getServiceItemId()).orElse(null);
            int duration = item == null || item.getDurationMinutes() == null ? 60 : item.getDurationMinutes();
            LocalDateTime oStart = o.getAppointmentTime();
            LocalDateTime oEnd = oStart.plusMinutes(duration);
            if (start.isBefore(oEnd) && oStart.isBefore(end)) {
                return true;
            }
        }
        return false;
    }

    private List<String> matchSkills(StaffProfile profile, String itemName, String categoryName) {
        List<String> matched = new ArrayList<>();
        String target = (itemName == null ? "" : itemName) + (categoryName == null ? "" : categoryName);
        for (String skill : profile.getSkillList()) {
            if (target.contains(skill) || skill.contains(itemName == null ? "" : itemName)) {
                matched.add(skill);
            }
        }
        return matched;
    }

    private String buildReason(boolean available, boolean capacity, boolean inWorkTime,
                               boolean noConflict, boolean onDuty, List<String> matched) {
        if (!onDuty) {
            return "当前休息中";
        }
        if (!capacity) {
            return "今日接单量已满";
        }
        if (!inWorkTime) {
            return "不在服务时段";
        }
        if (!noConflict) {
            return "该时段已有工单";
        }
        return matched.isEmpty() ? "可接单（技能未命中）" : "技能匹配：" + String.join("、", matched);
    }
}
