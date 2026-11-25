package com.community.controller.page;

import com.community.common.ratelimit.RateLimit;
import com.community.domain.OrderStatus;
import com.community.dto.OrderCreateForm;
import com.community.dto.ReviewForm;
import com.community.security.LoginUser;
import com.community.security.SecurityUtils;
import com.community.service.OrderService;
import com.community.service.ReviewService;
import com.community.vo.OrderView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 居民端：预约下单、我的订单、订单详情、取消、评价
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESIDENT','ADMIN')")
public class ResidentOrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    /** 提交预约单（表单来自服务详情页） */
    @RateLimit(key = "order:create", byUser = true, message = "预约提交过于频繁，请稍后再试")
    @PostMapping("/resident/orders")
    public String create(@Valid @ModelAttribute("form") OrderCreateForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getFieldErrors().stream()
                    .findFirst().map(e -> e.getDefaultMessage()).orElse("表单填写有误，请检查后重试"));
            return form.getItemId() == null ? "redirect:/services" : "redirect:/services/" + form.getItemId();
        }
        var order = orderService.createOrder(form, user.id());
        redirectAttributes.addFlashAttribute("success",
                "预约提交成功，订单号 " + order.getOrderNo() + "，我们会尽快为您派单");
        return "redirect:/resident/orders/" + order.getId();
    }

    @GetMapping("/resident/orders")
    public String myOrders(@RequestParam(required = false) OrderStatus status,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        LoginUser user = SecurityUtils.currentUser();
        Page<OrderView> orders = orderService.residentOrders(user.id(), status, page, 8);
        model.addAttribute("orders", orders);
        model.addAttribute("activeStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        return "resident/orders";
    }

    @GetMapping("/resident/orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        LoginUser user = SecurityUtils.currentUser();
        OrderView view = user.isAdmin()
                ? orderService.decorate(orderService.getOrder(id))
                : orderService.residentOrderDetail(id, user.id());
        var order = view.getOrder();
        model.addAttribute("view", view);
        model.addAttribute("logs", orderService.logs(id));
        model.addAttribute("review", reviewService.ofOrder(id).orElse(null));
        model.addAttribute("dispatchRecords", orderService.dispatchRecords(id));
        model.addAttribute("statuses", OrderStatus.values());
        if (!model.containsAttribute("reviewForm")) {
            ReviewForm reviewForm = new ReviewForm();
            reviewForm.setOrderId(id);
            model.addAttribute("reviewForm", reviewForm);
        }
        return "resident/order-detail";
    }

    @PostMapping("/resident/orders/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        try {
            var order = orderService.cancelByResident(id, reason, user.id());
            redirectAttributes.addFlashAttribute("success", "预约单 " + order.getOrderNo() + " 已取消");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/resident/orders/" + id;
    }

    @PostMapping("/resident/orders/{id}/review")
    public String review(@PathVariable Long id,
                         @Valid @ModelAttribute("reviewForm") ReviewForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        LoginUser user = SecurityUtils.currentUser();
        form.setOrderId(id);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getFieldErrors().stream()
                    .findFirst().map(e -> e.getDefaultMessage()).orElse("评价提交失败"));
            return "redirect:/resident/orders/" + id;
        }
        try {
            reviewService.submit(form, user.id());
            redirectAttributes.addFlashAttribute("success", "感谢您的评价，我们会持续改进服务");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/resident/orders/" + id;
    }
}
