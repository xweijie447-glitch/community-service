package com.community.domain;

/**
 * 系统角色：居民 / 服务人员 / 系统管理员
 */
public enum Role {

    RESIDENT("居民"),
    STAFF("服务人员"),
    ADMIN("系统管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Spring Security 角色名 */
    public String authority() {
        return "ROLE_" + name();
    }
}
