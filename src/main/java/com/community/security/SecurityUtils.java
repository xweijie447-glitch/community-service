package com.community.security;

import com.community.common.BusinessException;
import com.community.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    public static LoginUser currentUser() {
        LoginUser user = currentUserOrNull();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return user;
    }

    public static Long currentUserId() {
        LoginUser user = currentUserOrNull();
        return user == null ? null : user.id();
    }

    public static String currentDisplayName() {
        LoginUser user = currentUserOrNull();
        return user == null ? "系统" : user.getDisplayName();
    }

    public static boolean isAdmin() {
        LoginUser user = currentUserOrNull();
        return user != null && user.isAdmin();
    }
}
