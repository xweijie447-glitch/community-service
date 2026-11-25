package com.community.service;

import com.community.common.BusinessException;
import com.community.common.TxUtils;
import com.community.config.AppProperties;
import com.community.domain.DispatchRecord;
import com.community.domain.OrderStatus;
import com.community.domain.OrderStatusLog;
import com.community.domain.ServiceCategory;
import com.community.domain.ServiceItem;
import com.community.domain.ServiceOrder;
import com.community.domain.StaffProfile;
import com.community.domain.StaffStatus;
import com.community.domain.User;
import com.community.dto.OrderCreateForm;
import com.community.mq.OrderEventProducer;
import com.community.mq.message.NotifyMessage;
import com.community.repository.DispatchRecordRepository;
import com.community.repository.OrderStatusLogRepository;
import com.community.repository.ServiceCategoryRepository;
import com.community.repository.ServiceItemRepository;
import com.community.repository.ServiceOrderRepository;
import com.community.repository.StaffProfileRepository;
import com.community.repository.UserRepository;
import com.community.vo.OrderView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 预约订单主流程：下单、派单、状态流转、取消。
 * <p>幂等控制：</p>
 * <ol>
 *   <li>下单用 requestToken 去重，重复提交返回同一张单；写方法加锁，避免并发穿透；</li>
 *   <li>状态流转走状态机，重复调用同一目标状态视为成功（幂等返回）；每次流转 version +1。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ServiceOrderRepository orderRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final ServiceItemRepository itemRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final OrderEventProducer producer;
    private final AppProperties appProperties;

    /* ==================== 下单 ==================== */

    public synchronized ServiceOrder createOrder(OrderCreateForm form, Long residentId) {
        var existing = orderRepository.findByRequestToken(form.getRequestToken());
        if (existing.isPresent()) {
            log.info("重复提交被拦截，复用订单 {}", existing.get().getOrderNo());
            return existing.get();
        }

        ServiceItem item = itemRepository.findById(form.getItemId())
                .orElseThrow(() -> BusinessException.notFound("服务项目"));
        if (!item.isOnSale()) {
            throw new BusinessException("该服务项目已下架，请选择其他服务");
        }
        if (form.getAppointmentTime().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new BusinessException("预约时间需至少提前 30 分钟");
        }

        ServiceCategory category = categoryRepository.findById(item.getCategoryId()).orElse(null);
        int quantity = form.getQuantity() == null ? 1 : form.getQuantity();

        ServiceOrder order = new ServiceOrder();
        order.setOrderNo(generateOrderNo());
        order.setRequestToken(form.getRequestToken());
        order.setResidentId(residentId);
        order.setServiceItemId(item.getId());
        order.setServiceItemName(item.getName());
        order.setCategoryName(category == null ? "其他" : category.getName());
        order.setPrice(item.getPrice());
        order.setQuantity(quantity);
        order.setTotalAmount(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setAppointmentTime(form.getAppointmentTime());
        order.setAddress(form.getAddress().trim());
        order.setContactName(form.getContactName().trim());
        order.setContactPhone(form.getContactPhone().trim());
        order.setRemark(form.getRemark());
        order.setStatus(OrderStatus.PENDING);
        order.touch();

        final ServiceOrder saved = orderRepository.save(order);
        statusLogRepository.save(OrderStatusLog.of(saved, null, OrderStatus.PENDING, "居民", "提交预约单"));

        int timeoutMinutes = appProperties.getOrder().getTimeoutMinutes();
        TxUtils.afterCommit(() -> {
            producer.sendTimeoutCheck(saved.getId(), saved.getOrderNo(), timeoutMinutes);
            producer.sendNotify(saved.getId(), saved.getOrderNo(), NotifyMessage.ORDER_CREATED, null);
        });
        log.info("订单创建成功 orderNo={} item={} amount={}", saved.getOrderNo(), item.getName(),
                saved.getTotalAmount());
        return saved;
    }

    private String generateOrderNo() {
        return "CS" + LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /* ==================== 查询 ==================== */

    public ServiceOrder getOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> BusinessException.notFound("预约单"));
    }

    public ServiceOrder getOrderByNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo).orElseThrow(() -> BusinessException.notFound("预约单"));
    }

    public List<OrderStatusLog> logs(Long orderId) {
        return statusLogRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    public List<DispatchRecord> dispatchRecords(Long orderId) {
        return dispatchRecordRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    public Page<OrderView> residentOrders(Long residentId, OrderStatus status, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(Math.max(page, 0));
        Page<ServiceOrder> result = status == null
                ? orderRepository.findByResidentIdOrderByCreatedAtDesc(residentId, pageable)
                : orderRepository.findByResidentIdAndStatusOrderByCreatedAtDesc(residentId, status, pageable);
        return decorate(result);
    }

    public Page<OrderView> staffOrders(Long staffUserId, OrderStatus status, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(Math.max(page, 0));
        return decorate(orderRepository.findByStaff(staffUserId, status, pageable));
    }

    public List<ServiceOrder> staffActiveOrders(Long staffUserId) {
        return orderRepository.findByStaffIdAndStatusInOrderByAppointmentTimeAsc(staffUserId,
                List.of(OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE));
    }

    public Page<OrderView> adminSearch(OrderStatus status, String keyword, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(Math.max(page, 0));
        return decorate(orderRepository.search(status, keyword, pageable));
    }

    /** 补全居民 / 服务人员姓名，避免模板里再做查询 */
    public Page<OrderView> decorate(Page<ServiceOrder> page) {
        List<ServiceOrder> content = page.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }
        Map<Long, User> residents = userRepository.findAllById(
                        content.stream().map(ServiceOrder::getResidentId)
                                .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, User> staffs = userRepository.findAllById(
                        content.stream().map(ServiceOrder::getStaffId)
                                .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<OrderView> views = content.stream().map(order -> {
            User resident = residents.get(order.getResidentId());
            User staff = order.getStaffId() == null ? null : staffs.get(order.getStaffId());
            return new OrderView(order,
                    resident == null ? "-" : resident.getDisplayName(),
                    resident == null ? "-" : resident.getPhone(),
                    staff == null ? null : staff.getDisplayName(),
                    staff == null ? null : staff.getPhone());
        }).toList();
        return new PageImpl<>(views, page.getPageable(), page.getTotalElements());
    }

    public OrderView decorate(ServiceOrder order) {
        User resident = userRepository.findById(order.getResidentId()).orElse(null);
        User staff = order.getStaffId() == null ? null : userRepository.findById(order.getStaffId()).orElse(null);
        return new OrderView(order,
                resident == null ? "-" : resident.getDisplayName(),
                resident == null ? "-" : resident.getPhone(),
                staff == null ? null : staff.getDisplayName(),
                staff == null ? null : staff.getPhone());
    }

    public OrderView residentOrderDetail(Long orderId, Long residentId) {
        ServiceOrder order = getOrder(orderId);
        if (!order.getResidentId().equals(residentId)) {
            throw BusinessException.denied("无权查看他人的预约单");
        }
        return decorate(order);
    }

    /* ==================== 派单 ==================== */

    public synchronized ServiceOrder assign(Long orderId, Long staffUserId, String type,
                                            Double score, String reason) {
        ServiceOrder order = getOrder(orderId);
        if (order.getStatus().isFinal()) {
            throw BusinessException.stateConflict("订单已结束，无法派单");
        }
        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> BusinessException.notFound("服务人员"));
        StaffProfile profile = staffProfileRepository.findByUserId(staffUserId)
                .orElseThrow(() -> new BusinessException("该账号没有服务人员档案"));
        if (profile.getStatus() == StaffStatus.OFF) {
            throw new BusinessException("该服务人员当前休息中，无法派单");
        }

        OrderStatus from = order.getStatus();
        order.setStaffId(staffUserId);
        order.setAssignedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setVersion(order.getVersion() == null ? 1L : order.getVersion() + 1);
        order.touch();
        orderRepository.save(order);

        DispatchRecord record = new DispatchRecord();
        record.setOrderId(order.getId());
        record.setStaffId(staff.getId());
        record.setStaffName(staff.getDisplayName());
        record.setType(type);
        record.setMatchScore(score);
        record.setReason(reason);
        record.setCreatedAt(LocalDateTime.now());
        dispatchRecordRepository.save(record);

        statusLogRepository.save(OrderStatusLog.of(order, from, OrderStatus.ASSIGNED, "系统管理员",
                (from == OrderStatus.PENDING ? "派单给 " : "改派给 ") + staff.getDisplayName()));

        final ServiceOrder saved = order;
        TxUtils.afterCommit(() -> producer.sendNotify(saved.getId(), saved.getOrderNo(),
                NotifyMessage.DISPATCHED, staff.getDisplayName()));
        log.info("订单 {} 派单给 {} ({})", order.getOrderNo(), staff.getDisplayName(), type);
        return order;
    }

    /* ==================== 状态流转 ==================== */

    public synchronized ServiceOrder staffStart(Long orderId, Long staffUserId) {
        ServiceOrder order = requireStaffOrder(orderId, staffUserId);
        if (order.getStatus() == OrderStatus.IN_SERVICE) {
            return order;
        }
        return transit(order, OrderStatus.IN_SERVICE, "服务人员", "开始上门服务");
    }

    public synchronized ServiceOrder staffComplete(Long orderId, Long staffUserId) {
        ServiceOrder order = requireStaffOrder(orderId, staffUserId);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return order;
        }
        ServiceOrder completed = transit(order, OrderStatus.COMPLETED, "服务人员", "服务完成");

        itemRepository.findById(completed.getServiceItemId()).ifPresent(item -> {
            item.setSales(item.getSales() + completed.getQuantity());
            item.touch();
            itemRepository.save(item);
        });
        staffProfileRepository.findByUserId(staffUserId).ifPresent(profile -> {
            profile.setCompletedCount(profile.getCompletedCount() + 1);
            long active = orderRepository.countByStaffIdAndStatusIn(staffUserId,
                    List.of(OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE));
            if (active == 0) {
                profile.setStatus(StaffStatus.FREE);
            }
            profile.touch();
            staffProfileRepository.save(profile);
        });
        TxUtils.afterCommit(() -> producer.sendNotify(completed.getId(), completed.getOrderNo(),
                NotifyMessage.COMPLETED, null));
        return completed;
    }

    public synchronized ServiceOrder cancelByResident(Long orderId, String reason, Long residentId) {
        ServiceOrder order = getOrder(orderId);
        if (!order.getResidentId().equals(residentId)) {
            throw BusinessException.denied("无权操作他人的预约单");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        if (order.getStatus() == OrderStatus.IN_SERVICE || order.getStatus() == OrderStatus.COMPLETED) {
            throw BusinessException.stateConflict("服务已开始，无法取消，请联系物业处理");
        }
        ServiceOrder cancelled = transit(order, OrderStatus.CANCELLED, "居民",
                reason == null || reason.isBlank() ? "居民主动取消" : reason);
        TxUtils.afterCommit(() -> producer.sendNotify(cancelled.getId(), cancelled.getOrderNo(),
                NotifyMessage.CANCELLED, cancelled.getCancelReason()));
        return cancelled;
    }

    public synchronized ServiceOrder cancelByAdmin(Long orderId, String reason) {
        ServiceOrder order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw BusinessException.stateConflict("已完成的订单不能取消");
        }
        ServiceOrder cancelled = transit(order, OrderStatus.CANCELLED, "系统管理员",
                reason == null || reason.isBlank() ? "管理员取消" : reason);
        TxUtils.afterCommit(() -> producer.sendNotify(cancelled.getId(), cancelled.getOrderNo(),
                NotifyMessage.CANCELLED, cancelled.getCancelReason()));
        return cancelled;
    }

    /** MQ 超时消费：只有仍处于待接单状态才取消，否则视为已处理（幂等） */
    public synchronized void cancelByTimeout(Long orderId) {
        ServiceOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("超时取消失败：订单 {} 不存在", orderId);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("超时消息忽略：订单 {} 当前状态 {}", order.getOrderNo(), order.getStatus().getLabel());
            return;
        }
        ServiceOrder cancelled = transit(order, OrderStatus.CANCELLED, "系统", "超时未派单，系统自动取消");
        TxUtils.afterCommit(() -> producer.sendNotify(cancelled.getId(), cancelled.getOrderNo(),
                NotifyMessage.CANCELLED, "超时未派单自动取消"));
        log.info("订单 {} 超时未派单，已自动取消", order.getOrderNo());
    }

    /** 管理员手动改状态（受状态机约束） */
    public synchronized ServiceOrder adminTransit(Long orderId, OrderStatus target, String remark) {
        ServiceOrder order = getOrder(orderId);
        if (order.getStatus() == target) {
            return order;
        }
        return transit(order, target, "系统管理员", remark);
    }

    /** 状态机流转：非法流转抛业务异常，重复流转按幂等处理 */
    private ServiceOrder transit(ServiceOrder order, OrderStatus target, String operator, String remark) {
        OrderStatus from = order.getStatus();
        if (from == target) {
            log.info("幂等：订单 {} 已是 {} 状态", order.getOrderNo(), target.getLabel());
            return order;
        }
        if (!from.canTransitTo(target)) {
            throw BusinessException.stateConflict(
                    "订单当前为「%s」，不能变更为「%s」".formatted(from.getLabel(), target.getLabel()));
        }
        order.setStatus(target);
        order.setVersion(order.getVersion() == null ? 1L : order.getVersion() + 1);
        order.touch();
        switch (target) {
            case ASSIGNED -> order.setAssignedAt(LocalDateTime.now());
            case IN_SERVICE -> order.setStartedAt(LocalDateTime.now());
            case COMPLETED -> order.setCompletedAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelReason(remark);
            }
            default -> {
            }
        }
        ServiceOrder saved = orderRepository.save(order);
        statusLogRepository.save(OrderStatusLog.of(saved, from, target, operator, remark));
        log.info("订单 {} 状态流转 {} -> {} by {}", saved.getOrderNo(), from.getLabel(),
                target.getLabel(), operator);
        return saved;
    }

    private ServiceOrder requireStaffOrder(Long orderId, Long staffUserId) {
        ServiceOrder order = getOrder(orderId);
        if (!staffUserId.equals(order.getStaffId())) {
            throw BusinessException.denied("该工单未派给您，无法操作");
        }
        return order;
    }

    /* ==================== 统计辅助 ==================== */

    public long count(OrderStatus status) {
        return status == null ? orderRepository.count() : orderRepository.countByStatus(status);
    }

    public List<ServiceOrder> findTomorrowAssigned() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return orderRepository.findByStatusInAndAppointmentTimeBetween(
                List.of(OrderStatus.ASSIGNED, OrderStatus.IN_SERVICE), start, end);
    }
}
