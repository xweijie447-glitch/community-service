package com.community.repository;

import com.community.domain.OrderStatus;
import com.community.domain.ServiceOrder;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 预约单仓储（data/service_orders.json）
 */
@Repository
public class ServiceOrderRepository extends JsonRepository<ServiceOrder> {

    private static final Comparator<ServiceOrder> CREATED_DESC =
            Comparator.comparing(ServiceOrder::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ServiceOrder::getId, Comparator.reverseOrder());

    public ServiceOrderRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "service_orders", ServiceOrder.class);
    }

    public Optional<ServiceOrder> findByOrderNo(String orderNo) {
        return store.findAll().stream().filter(o -> orderNo != null && orderNo.equals(o.getOrderNo())).findFirst();
    }

    public Optional<ServiceOrder> findByRequestToken(String token) {
        return store.findAll().stream().filter(o -> token != null && token.equals(o.getRequestToken())).findFirst();
    }

    public boolean existsByRequestToken(String token) {
        return findByRequestToken(token).isPresent();
    }

    public Page<ServiceOrder> findByResidentIdOrderByCreatedAtDesc(Long residentId, Pageable pageable) {
        List<ServiceOrder> list = store.findAll().stream()
                .filter(o -> residentId.equals(o.getResidentId()))
                .sorted(CREATED_DESC)
                .toList();
        return page(list, pageable);
    }

    public Page<ServiceOrder> findByResidentIdAndStatusOrderByCreatedAtDesc(Long residentId, OrderStatus status,
                                                                           Pageable pageable) {
        List<ServiceOrder> list = store.findAll().stream()
                .filter(o -> residentId.equals(o.getResidentId()))
                .filter(o -> status == null || o.getStatus() == status)
                .sorted(CREATED_DESC)
                .toList();
        return page(list, pageable);
    }

    public Page<ServiceOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status, Pageable pageable) {
        List<ServiceOrder> list = store.findAll().stream()
                .filter(o -> status == null || o.getStatus() == status)
                .sorted(Comparator.comparing(ServiceOrder::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return page(list, pageable);
    }

    public List<ServiceOrder> findByStaffIdAndStatusInOrderByAppointmentTimeAsc(Long staffId,
                                                                               Collection<OrderStatus> statuses) {
        return store.findAll().stream()
                .filter(o -> staffId.equals(o.getStaffId()))
                .filter(o -> statuses.contains(o.getStatus()))
                .sorted(Comparator.comparing(ServiceOrder::getAppointmentTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<ServiceOrder> findByStaffIdOrderByCreatedAtDesc(Long staffId) {
        return store.findAll().stream()
                .filter(o -> staffId.equals(o.getStaffId()))
                .sorted(CREATED_DESC)
                .toList();
    }

    public List<ServiceOrder> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime time) {
        return store.findAll().stream()
                .filter(o -> o.getStatus() == status)
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isBefore(time))
                .toList();
    }

    public List<ServiceOrder> findByStatusInAndAppointmentTimeBetween(Collection<OrderStatus> statuses,
                                                                     LocalDateTime start, LocalDateTime end) {
        return store.findAll().stream()
                .filter(o -> statuses.contains(o.getStatus()))
                .filter(o -> o.getAppointmentTime() != null
                        && !o.getAppointmentTime().isBefore(start)
                        && o.getAppointmentTime().isBefore(end))
                .sorted(Comparator.comparing(ServiceOrder::getAppointmentTime))
                .toList();
    }

    public long countByStatus(OrderStatus status) {
        return store.findAll().stream().filter(o -> o.getStatus() == status).count();
    }

    public long countByStatusAndArchivedFalse(OrderStatus status) {
        return store.findAll().stream()
                .filter(o -> o.getStatus() == status && !Boolean.TRUE.equals(o.getArchived()))
                .count();
    }

    public long countByStaffIdAndStatusIn(Long staffId, Collection<OrderStatus> statuses) {
        return store.findAll().stream()
                .filter(o -> staffId.equals(o.getStaffId()))
                .filter(o -> statuses.contains(o.getStatus()))
                .count();
    }

    public long countByStaffIdAndStatus(Long staffId, OrderStatus status) {
        return store.findAll().stream()
                .filter(o -> staffId.equals(o.getStaffId()) && o.getStatus() == status)
                .count();
    }

    public List<ServiceOrder> findSince(LocalDateTime from) {
        return store.findAll().stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(from))
                .toList();
    }

    /** 管理端组合查询：状态 + 关键字 */
    public Page<ServiceOrder> search(OrderStatus status, String keyword, Pageable pageable) {
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        List<ServiceOrder> list = store.findAll().stream()
                .filter(o -> status == null || o.getStatus() == status)
                .filter(o -> kw == null || kw.isEmpty()
                        || contains(o.getOrderNo(), kw)
                        || contains(o.getServiceItemName(), kw)
                        || contains(o.getContactName(), kw)
                        || contains(o.getContactPhone(), kw)
                        || contains(o.getAddress(), kw))
                .sorted(CREATED_DESC)
                .toList();
        return page(list, pageable);
    }

    /** 最近 N 笔订单 */
    public List<ServiceOrder> findRecent(int limit) {
        return store.findAll().stream().sorted(CREATED_DESC).limit(Math.max(0, limit)).toList();
    }

    /** 服务人员工单分页 */
    public Page<ServiceOrder> findByStaff(Long staffId, OrderStatus status, Pageable pageable) {
        List<ServiceOrder> list = store.findAll().stream()
                .filter(o -> staffId.equals(o.getStaffId()))
                .filter(o -> status == null || o.getStatus() == status)
                .sorted(CREATED_DESC)
                .toList();
        return page(list, pageable);
    }

    public long countArchived() {
        return store.findAll().stream().filter(o -> Boolean.TRUE.equals(o.getArchived())).count();
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
    }
}
