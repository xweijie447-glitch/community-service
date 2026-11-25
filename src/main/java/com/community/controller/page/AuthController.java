package com.community.controller.page;

import com.community.domain.Role;
import com.community.dto.RegisterForm;
import com.community.repository.UserRepository;
import com.community.security.LoginUser;
import com.community.security.SecurityUtils;
import com.community.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 登录 / 注册
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/login")
    public String login(Model model) {
        LoginUser current = SecurityUtils.currentUserOrNull();
        if (current != null) {
            return "redirect:" + homeOf(current);
        }
        return "auth/login";
    }

    /** 拒绝访问页：POST 被安全框架 forward 过来时也要能渲染，否则会退化成 405 */
    @RequestMapping(value = "/403", method = {RequestMethod.GET, RequestMethod.POST})
    public String forbidden() {
        return "error/403";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "两次输入的密码不一致");
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "username.exists", "该账号已被注册");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        userService.register(form);
        redirectAttributes.addFlashAttribute("success", "注册成功，请使用新账号登录");
        return "redirect:/login";
    }

    private String homeOf(LoginUser user) {
        if (user.role() == Role.ADMIN) {
            return "/admin/dashboard";
        }
        if (user.role() == Role.STAFF) {
            return "/staff/workbench";
        }
        return "/";
    }
}
