package com.community.controller.page;

import com.community.domain.OrderStatus;
import com.community.domain.StaffProfile;
import com.community.domain.StaffStatus;
import com.community.security.LoginUser;
import com.community.security.SecurityUtils;
import com.community.service.OrderService;
import com.community.service.ReviewService;
import com.community.service.StaffService;
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
 * 服务人员工作台：我的工单、接单状态、技能维护、接单记录
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
public class StaffController {

    private final StaffService staffService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    @GetMapping("/staff/workbench")
    public String workbench(@RequestParam(defaultValue = "0") int page, Model model) {
        LoginUser user = SecurityUtils.currentUser();
        var view = staffService.getByUserId(user.id());
        model.addAttribute("staff", view);
        model.addAttribute("activeOrders", orderService.staffActiveOrders(user.id()));
        model.addAttribute("history", orderService.staffOrders(user.id(), null, page, 6));
        model.addAttribute("reviews", reviewService.latest().stream()
                .filter(r -> user.id().equals(r.getStaffId())).limit(5).toList());
        model.addAttribute("statuses", StaffStatus.values());
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "staff/workbench";
    }

    @PostMapping("/staff/status")
    public String updateStatus(@RequestParam StaffStatus status, RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        staffService.updateStatus(user.id(), status);
        redirectAttributes.addFlashAttribute("success", "接单状态已切换为「" + status.getLabel() + "」");
        return "redirect:/staff/workbench";
    }

    @PostMapping("/staff/profile")
    public String updateProfile(@RequestParam String skills,
                                @RequestParam(required = false) Integer maxDailyOrders,
                                @RequestParam(required = false) String intro,
                                RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        if (skills == null || skills.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "技能标签不能为空");
            return "redirect:/staff/workbench";
        }
        staffService.updateSelf(user.id(), skills, maxDailyOrders, intro);
        redirectAttributes.addFlashAttribute("success", "技能与接单配置已更新");
        return "redirect:/staff/workbench";
    }

    @PostMapping("/staff/orders/{id}/start")
    public String start(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        try {
            orderService.staffStart(id, user.id());
            redirectAttributes.addFlashAttribute("success", "已开始服务，请按预约时间上门");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/workbench";
    }

    @PostMapping("/staff/orders/{id}/complete")
    public String complete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        try {
            orderService.staffComplete(id, user.id());
            redirectAttributes.addFlashAttribute("success", "服务已完成，感谢辛苦付出");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/workbench";
    }

    @PostMapping("/staff/orders/{id}/off")
    public String setOff(RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        staffService.updateStatus(user.id(), StaffStatus.OFF);
        redirectAttributes.addFlashAttribute("success", "已切换为休息中，管理员将不会给您派单");
        return "redirect:/staff/workbench";
    }

    @GetMapping("/staff/orders")
    public String orders(@RequestParam(required = false) OrderStatus status,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        LoginUser user = SecurityUtils.currentUser();
        model.addAttribute("history", orderService.staffOrders(user.id(), status, page, 10));
        model.addAttribute("activeStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("staff", staffService.getByUserId(user.id()));
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "staff/orders";
    }

    @GetMapping("/staff/profile")
    public String profile(Model model) {
        LoginUser user = SecurityUtils.currentUser();
        StaffProfile profile = staffService.getByUserId(user.id()).getProfile();
        model.addAttribute("staff", staffService.getByUserId(user.id()));
        model.addAttribute("profile", profile);
        model.addAttribute("reviews", reviewService.latest().stream()
                .filter(r -> user.id().equals(r.getStaffId())).toList());
        return "staff/profile";
    }
}
