package com.community.repository;

import com.community.domain.ServiceCategory;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 服务分类仓储（data/service_categories.json）
 */
@Repository
public class ServiceCategoryRepository extends JsonRepository<ServiceCategory> {

    public ServiceCategoryRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "service_categories", ServiceCategory.class);
    }

    private static final Comparator<ServiceCategory> SORT = Comparator
            .comparing(ServiceCategory::getSortOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(ServiceCategory::getId);

    public List<ServiceCategory> findByEnabledTrueOrderBySortOrderAscIdAsc() {
        return store.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .sorted(SORT)
                .toList();
    }

    public List<ServiceCategory> findAllByOrderBySortOrderAscIdAsc() {
        return store.findAll().stream().sorted(SORT).toList();
    }

    public Optional<ServiceCategory> findByCode(String code) {
        return store.findAll().stream().filter(c -> code != null && code.equals(c.getCode())).findFirst();
    }

    public boolean existsByCode(String code) {
        return findByCode(code).isPresent();
    }
}
