package com.community.security;

import com.community.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * 登录态用户（Session 中保存的安全主体）。
 * <p>补充 getXxx 形式的方法，方便 Thymeleaf / SpEL 直接读属性。</p>
 */
public record LoginUser(Long id,
                        String username,
                        String password,
                        String realName,
                        Role role,
                        boolean enabled) implements org.springframework.security.core.userdetails.UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getUserId() {
        return id;
    }

    public String getRealName() {
        return realName;
    }

    public Role getRole() {
        return role;
    }

    public String getRoleLabel() {
        return role == null ? "" : role.getLabel();
    }

    public String getDisplayName() {
        return realName == null || realName.isBlank() ? username : realName;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStaff() {
        return role == Role.STAFF;
    }

    public boolean isResident() {
        return role == Role.RESIDENT;
    }
}
