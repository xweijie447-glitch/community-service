package com.community.service;

import com.community.common.BusinessException;
import com.community.common.CacheNames;
import com.community.domain.ItemStatus;
import com.community.domain.ServiceCategory;
import com.community.domain.ServiceItem;
import com.community.dto.CategoryForm;
import com.community.dto.ServiceItemForm;
import com.community.repository.ServiceCategoryRepository;
import com.community.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 服务管理：分类、服务项目、详情。
 * <p>分类与热门项目写入 Redis，后台变更后统一清理缓存。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository itemRepository;

    /* ==================== 分类 ==================== */

    /** 居民端可见分类，走 Redis 缓存 */
    @Cacheable(cacheNames = CacheNames.SERVICE_CATEGORIES, key = "'enabled'")
    public List<ServiceCategory> enabledCategories() {
        log.debug("分类缓存未命中，查询数据库");
        return categoryRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    public List<ServiceCategory> allCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    public Map<Long, ServiceCategory> categoryMap() {
        return allCategories().stream()
                .collect(Collectors.toMap(ServiceCategory::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public ServiceCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("服务分类"));
    }

    @CacheEvict(cacheNames = {CacheNames.SERVICE_CATEGORIES, CacheNames.HOT_ITEMS,
            CacheNames.ITEM_DETAIL}, allEntries = true)
    public ServiceCategory saveCategory(CategoryForm form) {
        ServiceCategory category = form.getId() == null
                ? new ServiceCategory()
                : getCategory(form.getId());
        if (form.getId() == null && categoryRepository.existsByCode(form.getCode())) {
            throw new BusinessException("分类编码已存在：" + form.getCode());
        }
        category.setCode(form.getCode().trim());
        category.setName(form.getName().trim());
        category.setIcon(form.getIcon());
        category.setDescription(form.getDescription());
        category.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());
        category.setEnabled(form.getEnabled() == null || form.getEnabled());
        log.info("保存服务分类 {} - {}", category.getCode(), category.getName());
        return categoryRepository.save(category);
    }

    @CacheEvict(cacheNames = {CacheNames.SERVICE_CATEGORIES, CacheNames.HOT_ITEMS,
            CacheNames.ITEM_DETAIL}, allEntries = true)
    public void deleteCategory(Long id) {
        ServiceCategory category = getCategory(id);
        if (!itemRepository.findByCategoryIdOrderByHotDescSalesDesc(id).isEmpty()) {
            throw new BusinessException("该分类下仍有服务项目，请先移除或转移");
        }
        categoryRepository.delete(category);
        log.info("删除服务分类 {}", category.getName());
    }

    /* ==================== 服务项目 ==================== */

    /** 热门服务项目（Top 6），走 Redis 缓存 */
    @Cacheable(cacheNames = CacheNames.HOT_ITEMS, key = "'top6'")
    public List<ServiceItem> hotItems() {
        log.debug("热门服务缓存未命中，查询数据库");
        return itemRepository.findByHotTrueAndStatusOrderBySalesDescIdDesc(
                ItemStatus.ON_SALE, PageRequest.of(0, 6));
    }

    @Cacheable(cacheNames = CacheNames.ITEM_DETAIL, key = "#id")
    public ServiceItem getOnSaleItem(Long id) {
        ServiceItem item = itemRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("服务项目"));
        if (!item.isOnSale()) {
            throw new BusinessException("该服务已下架，无法预约");
        }
        return item;
    }

    public ServiceItem getItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> BusinessException.notFound("服务项目"));
    }

    public List<ServiceItem> listOnSale(Long categoryId) {
        if (categoryId == null) {
            return itemRepository.findByStatusOrderByHotDescSalesDescIdDesc(ItemStatus.ON_SALE);
        }
        return itemRepository.findByCategoryIdAndStatusOrderByHotDescSalesDesc(categoryId, ItemStatus.ON_SALE);
    }

    public Page<ServiceItem> search(Long categoryId, String keyword, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return itemRepository.search(categoryId, kw, ItemStatus.ON_SALE,
                PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "hot", "sales", "id")));
    }

    public Page<ServiceItem> adminSearch(Long categoryId, String keyword, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return itemRepository.search(categoryId, kw, null,
                PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "id")));
    }

    public List<ServiceItem> itemsOfCategory(Long categoryId) {
        return itemRepository.findByCategoryIdOrderByHotDescSalesDesc(categoryId);
    }

    @CacheEvict(cacheNames = {CacheNames.HOT_ITEMS, CacheNames.ITEM_DETAIL,
            CacheNames.SERVICE_CATEGORIES}, allEntries = true)
    public ServiceItem saveItem(ServiceItemForm form) {
        getCategory(form.getCategoryId());
        ServiceItem item = form.getId() == null ? new ServiceItem() : getItem(form.getId());
        item.setCategoryId(form.getCategoryId());
        item.setName(form.getName().trim());
        item.setSubtitle(form.getSubtitle());
        item.setDescription(form.getDescription());
        item.setPrice(form.getPrice());
        item.setUnit(form.getUnit());
        item.setDurationMinutes(form.getDurationMinutes());
        item.setHot(form.getHot() != null && form.getHot());
        item.setStatus(form.getStatus() == null ? ItemStatus.ON_SALE : form.getStatus());
        log.info("保存服务项目 {} - {} 元/{}", item.getName(), item.getPrice(), item.getUnit());
        return itemRepository.save(item);
    }

    @CacheEvict(cacheNames = {CacheNames.HOT_ITEMS, CacheNames.ITEM_DETAIL,
            CacheNames.SERVICE_CATEGORIES}, allEntries = true)
    public void toggleItemStatus(Long id) {
        ServiceItem item = getItem(id);
        item.setStatus(item.isOnSale() ? ItemStatus.OFF_SALE : ItemStatus.ON_SALE);
    }

    @CacheEvict(cacheNames = {CacheNames.HOT_ITEMS, CacheNames.ITEM_DETAIL}, allEntries = true)
    public void toggleHot(Long id) {
        ServiceItem item = getItem(id);
        item.setHot(!Boolean.TRUE.equals(item.getHot()));
    }

    /** 缓存预热：应用启动后把分类与热门项目写入 Redis */
    public void warmUp() {
        enabledCategories();
        hotItems();
        log.info("服务分类 / 热门服务缓存预热完成");
    }

    public Pageable top(int size) {
        return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "sales"));
    }
}
