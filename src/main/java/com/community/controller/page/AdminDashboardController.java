package com.community.controller.page;

import com.community.domain.OrderStatus;
import com.community.repository.MessageLogRepository;
import com.community.repository.ServiceItemRepository;
import com.community.service.CatalogService;
import com.community.service.OrderService;
import com.community.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 管理端运营看板
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final StatisticsService statisticsService;
    private final OrderService orderService;
    private final CatalogService catalogService;
    private final MessageLogRepository messageLogRepository;
    private final ServiceItemRepository itemRepository;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("overview", statisticsService.overview());
        model.addAttribute("trend", statisticsService.trend(7));
        model.addAttribute("categoryShare", statisticsService.categoryShare());
        model.addAttribute("statusDistribution", statisticsService.statusDistribution());
        model.addAttribute("staffPerformance", statisticsService.staffPerformance());
        model.addAttribute("recentOrders", orderService.decorate(new org.springframework.data.domain.PageImpl<>(
                statisticsService.recentOrders(6))).getContent());
        model.addAttribute("messages", messageLogRepository.findTop100ByOrderByIdDesc().stream().limit(6).toList());
        model.addAttribute("categories", catalogService.enabledCategories());
        model.addAttribute("itemCount", itemRepository.count());
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/dashboard";
    }
}
