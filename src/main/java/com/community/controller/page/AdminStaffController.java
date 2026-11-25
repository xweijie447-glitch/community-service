package com.community.controller.page;

import com.community.domain.StaffStatus;
import com.community.dto.StaffForm;
import com.community.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 管理端：服务人员管理
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final StaffService staffService;

    @GetMapping("/admin/staff")
    public String list(Model model) {
        model.addAttribute("staffList", staffService.listAll());
        model.addAttribute("statuses", StaffStatus.values());
        return "admin/staff";
    }

    @GetMapping("/admin/staff/new")
    public String create(Model model) {
        model.addAttribute("form", new StaffForm());
        model.addAttribute("statuses", StaffStatus.values());
        model.addAttribute("editing", false);
        return "admin/staff-form";
    }

    @GetMapping("/admin/staff/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", staffService.toForm(id));
        model.addAttribute("statuses", StaffStatus.values());
        model.addAttribute("editing", true);
        return "admin/staff-form";
    }

    @PostMapping("/admin/staff/save")
    public String save(@Valid @ModelAttribute("form") StaffForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (form.getUserId() == null && (form.getPassword() == null || form.getPassword().isBlank())) {
            bindingResult.rejectValue("password", "password.required", "新增服务人员必须设置初始密码");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", StaffStatus.values());
            model.addAttribute("editing", form.getUserId() != null);
            model.addAttribute("errorMessage", bindingResult.getFieldErrors().stream()
                    .findFirst().map(e -> e.getDefaultMessage()).orElse("表单校验失败"));
            return "admin/staff-form";
        }
        try {
            staffService.saveStaff(form);
            redirectAttributes.addFlashAttribute("success", "服务人员信息已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        staffService.toggleEnabled(id);
        redirectAttributes.addFlashAttribute("success", "账号启用状态已切换");
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{id}/status")
    public String status(@PathVariable Long id, @ModelAttribute StaffStatus status,
                         RedirectAttributes redirectAttributes) {
        staffService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("success", "接单状态已更新");
        return "redirect:/admin/staff";
    }
}
