package com.community.repository;

import com.community.domain.MessageLog;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

/**
 * 消息记录仓储（data/message_logs.json）
 */
@Repository
public class MessageLogRepository extends JsonRepository<MessageLog> {

    public MessageLogRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "message_logs", MessageLog.class);
    }

    public List<MessageLog> findTop100ByOrderByIdDesc() {
        return store.findAll().stream()
                .sorted(Comparator.comparing(MessageLog::getId).reversed())
                .limit(100)
                .toList();
    }

    public List<MessageLog> findByOrderNoOrderByIdAsc(String orderNo) {
        return store.findAll().stream()
                .filter(m -> orderNo != null && orderNo.equals(m.getOrderNo()))
                .sorted(Comparator.comparing(MessageLog::getId))
                .toList();
    }

    public long countByStatus(String status) {
        return store.findAll().stream().filter(m -> status.equals(m.getStatus())).count();
    }
}
