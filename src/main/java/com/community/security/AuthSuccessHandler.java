package com.community.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 登录成功后按角色分流：管理员进看板、服务人员进工作台、居民进首页
 */
@Slf4j
@Component
public class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof LoginUser user)) {
            return "/";
        }
        return switch (user.role()) {
            case ADMIN -> "/admin/dashboard";
            case STAFF -> "/staff/workbench";
            case RESIDENT -> "/";
        };
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String target = determineTargetUrl(request, response, authentication);
        log.info("用户 {} 登录成功，跳转 {}", authentication.getName(), target);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
