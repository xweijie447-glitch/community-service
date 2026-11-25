package com.community.repository;

import com.community.domain.StaffProfile;
import com.community.domain.StaffStatus;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 服务人员档案仓储（data/staff_profiles.json）
 */
@Repository
public class StaffProfileRepository extends JsonRepository<StaffProfile> {

    public StaffProfileRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "staff_profiles", StaffProfile.class);
    }

    public Optional<StaffProfile> findByUserId(Long userId) {
        return store.findAll().stream().filter(p -> p.getUserId().equals(userId)).findFirst();
    }

    public List<StaffProfile> findByStatus(StaffStatus status) {
        return store.findAll().stream().filter(p -> p.getStatus() == status).toList();
    }

    /** 可接单优先、评分高的排前面 */
    public List<StaffProfile> findAllByOrderByStatusAscAvgScoreDesc() {
        return store.findAll().stream()
                .sorted(Comparator.comparing((StaffProfile p) -> p.getStatus() == StaffStatus.FREE ? 0 : 1)
                        .thenComparing(StaffProfile::getAvgScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StaffProfile::getId))
                .toList();
    }

    public long countByStatus(StaffStatus status) {
        return store.findAll().stream().filter(p -> p.getStatus() == status).count();
    }
}
