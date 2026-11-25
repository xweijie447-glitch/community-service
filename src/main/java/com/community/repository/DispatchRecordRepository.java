package com.community.repository;

import com.community.domain.DispatchRecord;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

/**
 * 派单记录仓储（data/dispatch_records.json）
 */
@Repository
public class DispatchRecordRepository extends JsonRepository<DispatchRecord> {

    public DispatchRecordRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "dispatch_records", DispatchRecord.class);
    }

    public List<DispatchRecord> findByOrderIdOrderByIdAsc(Long orderId) {
        return store.findAll().stream()
                .filter(r -> orderId.equals(r.getOrderId()))
                .sorted(Comparator.comparing(DispatchRecord::getId))
                .toList();
    }
}
