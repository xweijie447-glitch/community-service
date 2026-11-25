package com.community.controller.page;

import com.community.domain.User;
import com.community.dto.PasswordForm;
import com.community.dto.ProfileForm;
import com.community.security.LoginUser;
import com.community.security.SecurityUtils;
import com.community.service.OrderService;
import com.community.service.ReviewService;
import com.community.service.UserService;
import com.community.domain.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 用户中心：个人信息、预约历史、我的评价、修改密码
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESIDENT','ADMIN')")
public class ProfileController {

    private final UserService userService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    @GetMapping("/resident/profile")
    public String profile(Model model) {
        LoginUser user = SecurityUtils.currentUser();
        User entity = userService.get(user.id());
        model.addAttribute("user", entity);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ProfileForm.from(entity));
        }
        model.addAttribute("passwordForm", new PasswordForm());
        model.addAttribute("recentOrders", orderService.residentOrders(user.id(), null, 0, 5));
        model.addAttribute("completedCount", orderService.residentOrders(user.id(),
                OrderStatus.COMPLETED, 0, 1).getTotalElements());
        model.addAttribute("reviewCount", reviewService.ofResident(user.id()).size());
        return "resident/profile";
    }

    @PostMapping("/resident/profile")
    public String updateProfile(@Valid @ModelAttribute("form") ProfileForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getFieldErrors().stream()
                    .findFirst().map(e -> e.getDefaultMessage()).orElse("资料保存失败"));
            return "redirect:/resident/profile";
        }
        userService.updateProfile(SecurityUtils.currentUser().id(), form);
        redirectAttributes.addFlashAttribute("success", "个人资料已更新");
        return "redirect:/resident/profile";
    }

    @PostMapping("/resident/profile/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getFieldErrors().stream()
                    .findFirst().map(e -> e.getDefaultMessage()).orElse("密码修改失败"));
            return "redirect:/resident/profile";
        }
        try {
            userService.changePassword(SecurityUtils.currentUser().id(), form);
            redirectAttributes.addFlashAttribute("success", "密码修改成功，请重新登录");
            return "redirect:/logout";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/resident/profile";
        }
    }

    @GetMapping("/resident/reviews")
    public String myReviews(Model model) {
        LoginUser user = SecurityUtils.currentUser();
        model.addAttribute("reviews", reviewService.ofResident(user.id()));
        return "resident/reviews";
    }
}
