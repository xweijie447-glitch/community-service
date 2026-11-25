package com.community.controller.page;

import com.community.domain.OrderStatus;
import com.community.service.DispatchService;
import com.community.service.OrderService;
import com.community.service.StaffService;
import com.community.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 管理端：派单管理与订单状态调整
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final DispatchService dispatchService;
    private final StaffService staffService;
    private final ReviewService reviewService;

    @GetMapping("/admin/orders")
    public String orders(@RequestParam(required = false) OrderStatus status,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        model.addAttribute("orders", orderService.adminSearch(status, keyword, page, 10));
        model.addAttribute("activeStatus", status);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("pendingCount", orderService.count(OrderStatus.PENDING));
        return "admin/orders";
    }

    @GetMapping("/admin/orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var view = orderService.decorate(orderService.getOrder(id));
        model.addAttribute("view", view);
        model.addAttribute("logs", orderService.logs(id));
        model.addAttribute("dispatchRecords", orderService.dispatchRecords(id));
        model.addAttribute("review", reviewService.ofOrder(id).orElse(null));
        model.addAttribute("candidates", dispatchService.recommend(view.getOrder()));
        model.addAttribute("staffList", staffService.listAll());
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/order-detail";
    }

    @PostMapping("/admin/orders/{id}/auto-assign")
    public String autoAssign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            var order = dispatchService.autoAssign(id);
            redirectAttributes.addFlashAttribute("success", "已智能派单给 " + order.getStaffId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long staffId,
                         @RequestParam(required = false) String remark,
                         RedirectAttributes redirectAttributes) {
        try {
            dispatchService.manualAssign(id, staffId, remark);
            redirectAttributes.addFlashAttribute("success", "派单成功，已通知服务人员");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/transit")
    public String transit(@PathVariable Long id, @RequestParam OrderStatus target,
                          @RequestParam(required = false) String remark,
                          RedirectAttributes redirectAttributes) {
        try {
            orderService.adminTransit(id, target, remark);
            redirectAttributes.addFlashAttribute("success", "订单状态已更新为「" + target.getLabel() + "」");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelByAdmin(id, reason);
            redirectAttributes.addFlashAttribute("success", "订单已取消");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
