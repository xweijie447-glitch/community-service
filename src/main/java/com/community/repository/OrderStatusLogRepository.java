package com.community.repository;

import com.community.domain.OrderStatusLog;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

/**
 * 订单状态流水仓储（data/order_status_logs.json）
 */
@Repository
public class OrderStatusLogRepository extends JsonRepository<OrderStatusLog> {

    public OrderStatusLogRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "order_status_logs", OrderStatusLog.class);
    }

    public List<OrderStatusLog> findByOrderIdOrderByIdAsc(Long orderId) {
        return store.findAll().stream()
                .filter(l -> orderId.equals(l.getOrderId()))
                .sorted(Comparator.comparing(OrderStatusLog::getId))
                .toList();
    }
}
