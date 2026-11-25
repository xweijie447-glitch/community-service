package com.community.service;

import com.community.domain.OrderStatus;
import com.community.domain.ServiceOrder;
import com.community.domain.StaffProfile;
import com.community.domain.Role;
import com.community.domain.User;
import com.community.repository.ReviewRepository;
import com.community.repository.ServiceOrderRepository;
import com.community.repository.StaffProfileRepository;
import com.community.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 运营数据看板统计
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ServiceOrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;

    @Getter
    @AllArgsConstructor
    public static class Overview {
        private final long totalOrders;
        private final long todayOrders;
        private final long pending;
        private final long assigned;
        private final long inService;
        private final long completed;
        private final long cancelled;
        private final BigDecimal todayRevenue;
        private final BigDecimal monthRevenue;
        private final long residents;
        private final long staffs;
        private final double avgScore;
    }

    public Overview overview() {
        LocalDate today = LocalDate.now();
        List<ServiceOrder> recent = orderRepository.findSince(today.minusDays(30).atStartOfDay());

        long todayOrders = recent.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(today))
                .count();
        BigDecimal todayRevenue = recent.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED && o.getCompletedAt() != null
                        && o.getCompletedAt().toLocalDate().equals(today))
                .map(ServiceOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthRevenue = recent.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(ServiceOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgScore = reviewRepository.findAll().stream()
                .mapToInt(r -> r.getScore() == null ? 5 : r.getScore())
                .average().orElse(5.0);

        return new Overview(orderRepository.count(), todayOrders,
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.countByStatus(OrderStatus.ASSIGNED),
                orderRepository.countByStatus(OrderStatus.IN_SERVICE),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                todayRevenue, monthRevenue,
                userRepository.countByRole(Role.RESIDENT),
                userRepository.countByRole(Role.STAFF),
                Math.round(avgScore * 10) / 10.0);
    }

    /** 近 N 天订单量趋势 */
    public List<Map<String, Object>> trend(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        Map<LocalDate, long[]> bucket = new TreeMap<>();
        for (int i = 0; i < days; i++) {
            bucket.put(from.plusDays(i), new long[]{0, 0});
        }
        for (ServiceOrder order : orderRepository.findSince(from.atStartOfDay())) {
            if (order.getCreatedAt() == null) {
                continue;
            }
            LocalDate date = order.getCreatedAt().toLocalDate();
            long[] counter = bucket.get(date);
            if (counter != null) {
                counter[0]++;
                if (order.getStatus() == OrderStatus.COMPLETED) {
                    counter[1]++;
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        bucket.forEach((date, counter) -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.toString().substring(5));
            point.put("total", counter[0]);
            point.put("completed", counter[1]);
            result.add(point);
        });
        return result;
    }

    /** 服务类型占比 */
    public List<Map<String, Object>> categoryShare() {
        Map<String, Long> counter = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        o -> o.getCategoryName() == null ? "其他" : o.getCategoryName(),
                        Collectors.counting()));
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                }).toList();
    }

    /** 订单状态分布 */
    public List<Map<String, Object>> statusDistribution() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(ServiceOrder::getStatus, Collectors.counting()))
                .entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().ordinal()))
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey().getLabel());
                    item.put("value", e.getValue());
                    return item;
                }).toList();
    }

    /** 服务人员绩效：完成单量、评分、在手单量 */
    public List<Map<String, Object>> staffPerformance() {
        Map<Long, User> users = userRepository.findAllById(
                        staffProfileRepository.findAll().stream().map(StaffProfile::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, BigDecimal> revenue = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED && o.getStaffId() != null)
                .collect(Collectors.groupingBy(ServiceOrder::getStaffId,
                        Collectors.reducing(BigDecimal.ZERO, ServiceOrder::getTotalAmount, BigDecimal::add)));

        return staffProfileRepository.findAllByOrderByStatusAscAvgScoreDesc().stream().map(profile -> {
            User user = users.get(profile.getUserId());
            long active = orderRepository.countByStaffIdAndStatusIn(profile.getUserId(),
                    List.of(OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", user == null ? "-" : user.getDisplayName());
            row.put("completed", profile.getCompletedCount());
            row.put("score", profile.getAvgScore());
            row.put("active", active);
            row.put("revenue", revenue.getOrDefault(profile.getUserId(), BigDecimal.ZERO));
            row.put("status", profile.getStatus().getLabel());
            return row;
        }).toList();
    }

    public long pendingCount() {
        return orderRepository.countByStatus(OrderStatus.PENDING);
    }

    public List<ServiceOrder> recentOrders(int limit) {
        return orderRepository.findRecent(limit);
    }

    public String todayLabel() {
        return LocalDateTime.now().toLocalDate().toString();
    }
}
