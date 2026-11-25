package com.community.controller.page;

import com.community.domain.ServiceCategory;
import com.community.domain.ServiceItem;
import com.community.dto.CategoryForm;
import com.community.dto.ServiceItemForm;
import com.community.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端：服务分类与服务项目管理
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CatalogService catalogService;

    /* ==================== 分类 ==================== */

    @GetMapping("/admin/categories")
    public String categories(Model model) {
        model.addAttribute("categories", catalogService.allCategories());
        model.addAttribute("counts", catalogService.allCategories().stream()
                .collect(Collectors.toMap(ServiceCategory::getId,
                        c -> (long) catalogService.itemsOfCategory(c.getId()).size())));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CategoryForm());
        }
        return "admin/categories";
    }

    @PostMapping("/admin/categories/save")
    public String saveCategory(@Valid @ModelAttribute("form") CategoryForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getFieldErrors().stream()
                    .findFirst().map(e -> e.getDefaultMessage()).orElse("保存失败"));
            return "redirect:/admin/categories";
        }
        try {
            catalogService.saveCategory(form);
            redirectAttributes.addFlashAttribute("success", "分类已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            catalogService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "分类已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /* ==================== 服务项目 ==================== */

    @GetMapping("/admin/items")
    public String items(@RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<ServiceItem> items = catalogService.adminSearch(categoryId, keyword, page, 10);
        Map<Long, String> categoryNames = catalogService.categoryMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
        model.addAttribute("items", items);
        model.addAttribute("categories", catalogService.allCategories());
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("activeCategory", categoryId);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "admin/items";
    }

    @GetMapping("/admin/items/new")
    public String newItem(Model model) {
        ServiceItemForm form = new ServiceItemForm();
        model.addAttribute("form", form);
        model.addAttribute("categories", catalogService.allCategories());
        return "admin/item-form";
    }

    @GetMapping("/admin/items/{id}/edit")
    public String editItem(@PathVariable Long id, Model model) {
        model.addAttribute("form", ServiceItemForm.from(catalogService.getItem(id)));
        model.addAttribute("categories", catalogService.allCategories());
        return "admin/item-form";
    }

    @PostMapping("/admin/items/save")
    public String saveItem(@Valid @ModelAttribute("form") ServiceItemForm form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", catalogService.allCategories());
            model.addAttribute("errorMessage", bindingResult.getFieldErrors().stream()
                    .findFirst().map(e -> e.getDefaultMessage()).orElse("表单校验失败"));
            return "admin/item-form";
        }
        try {
            catalogService.saveItem(form);
            redirectAttributes.addFlashAttribute("success", "服务项目已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/items";
    }

    @PostMapping("/admin/items/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        catalogService.toggleItemStatus(id);
        redirectAttributes.addFlashAttribute("success", "上下架状态已切换");
        return "redirect:/admin/items";
    }

    @PostMapping("/admin/items/{id}/toggle-hot")
    public String toggleHot(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        catalogService.toggleHot(id);
        redirectAttributes.addFlashAttribute("success", "热门标记已切换");
        return "redirect:/admin/items";
    }
}
