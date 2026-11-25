package com.community.repository;

import com.community.domain.ItemStatus;
import com.community.domain.ServiceItem;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

/**
 * 服务项目仓储（data/service_items.json）
 */
@Repository
public class ServiceItemRepository extends JsonRepository<ServiceItem> {

    private static final Comparator<ServiceItem> HOT_FIRST = Comparator
            .comparing((ServiceItem i) -> Boolean.TRUE.equals(i.getHot()) ? 1 : 0, Comparator.reverseOrder())
            .thenComparing(ServiceItem::getSales, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ServiceItem::getId, Comparator.reverseOrder());

    public ServiceItemRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "service_items", ServiceItem.class);
    }

    public List<ServiceItem> findByStatusOrderByHotDescSalesDescIdDesc(ItemStatus status) {
        return store.findAll().stream()
                .filter(i -> status == null || i.getStatus() == status)
                .sorted(HOT_FIRST)
                .toList();
    }

    public List<ServiceItem> findByCategoryIdAndStatusOrderByHotDescSalesDesc(Long categoryId, ItemStatus status) {
        return store.findAll().stream()
                .filter(i -> categoryId.equals(i.getCategoryId()))
                .filter(i -> status == null || i.getStatus() == status)
                .sorted(HOT_FIRST)
                .toList();
    }

    public List<ServiceItem> findByHotTrueAndStatusOrderBySalesDescIdDesc(ItemStatus status, Pageable pageable) {
        List<ServiceItem> list = store.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getHot()))
                .filter(i -> status == null || i.getStatus() == status)
                .sorted(Comparator.comparing(ServiceItem::getSales,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int limit = pageable == null ? list.size() : pageable.getPageSize();
        return list.stream().limit(limit).toList();
    }

    public List<ServiceItem> findByCategoryIdOrderByHotDescSalesDesc(Long categoryId) {
        return store.findAll().stream()
                .filter(i -> categoryId.equals(i.getCategoryId()))
                .sorted(HOT_FIRST)
                .toList();
    }

    /** 关键字 + 分类 + 状态组合查询，status 传 null 表示不限 */
    public Page<ServiceItem> search(Long categoryId, String keyword, ItemStatus status, Pageable pageable) {
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        List<ServiceItem> list = store.findAll().stream()
                .filter(i -> categoryId == null || categoryId.equals(i.getCategoryId()))
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> kw == null || kw.isEmpty()
                        || (i.getName() != null && i.getName().toLowerCase().contains(kw))
                        || (i.getSubtitle() != null && i.getSubtitle().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(ServiceItem::getId).reversed())
                .toList();
        return page(list, pageable);
    }

    public long countByStatus(ItemStatus status) {
        return store.findAll().stream().filter(i -> i.getStatus() == status).count();
    }
}
