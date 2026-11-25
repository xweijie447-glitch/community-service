package com.community.controller.page;

import com.community.domain.ServiceCategory;
import com.community.domain.ServiceItem;
import com.community.domain.User;
import com.community.repository.ServiceCategoryRepository;
import com.community.repository.UserRepository;
import com.community.security.LoginUser;
import com.community.security.SecurityUtils;
import com.community.service.CatalogService;
import com.community.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 居民端首页与服务大厅
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CatalogService catalogService;
    private final ServiceCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StatisticsService statisticsService;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        List<ServiceCategory> categories = catalogService.enabledCategories();
        List<ServiceItem> hotItems = catalogService.hotItems();
        Map<Long, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName));

        model.addAttribute("categories", categories);
        model.addAttribute("hotItems", hotItems);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("overview", statisticsService.overview());
        return "index";
    }

    @GetMapping("/services")
    public String services(@RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        Page<ServiceItem> items = catalogService.search(categoryId, keyword, page, 9);
        Map<Long, String> categoryNames = catalogService.categoryMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));

        model.addAttribute("items", items);
        model.addAttribute("categories", catalogService.enabledCategories());
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("activeCategory", categoryId);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "service/list";
    }

    @GetMapping("/services/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ServiceItem item = catalogService.getOnSaleItem(id);
        ServiceCategory category = categoryRepository.findById(item.getCategoryId()).orElse(null);

        String defaultAddress = null;
        LoginUser current = SecurityUtils.currentUserOrNull();
        if (current != null) {
            defaultAddress = userRepository.findById(current.id()).map(User::getAddress).orElse(null);
        }

        model.addAttribute("item", item);
        model.addAttribute("category", category);
        model.addAttribute("defaultAddress", defaultAddress);
        model.addAttribute("requestToken", UUID.randomUUID().toString());
        model.addAttribute("minTime", LocalDateTime.now().plusHours(1).withSecond(0).withNano(0));
        model.addAttribute("related", catalogService.listOnSale(item.getCategoryId()).stream()
                .filter(i -> !i.getId().equals(item.getId())).limit(3).toList());
        return "service/detail";
    }
}
