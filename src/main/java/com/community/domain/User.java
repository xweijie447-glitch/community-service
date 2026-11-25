package com.community.domain;

import com.community.storage.HasId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 系统用户（居民 / 服务人员 / 管理员统一账号）
 */
@Getter
@Setter
@ToString(exclude = "password")
@NoArgsConstructor
public class User implements HasId {

    private Long id;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private Role role = Role.RESIDENT;

    /** 居民默认服务地址 */
    private String address;

    private Boolean enabled = Boolean.TRUE;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void touch() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @JsonIgnore
    public String getDisplayName() {
        return realName == null || realName.isBlank() ? username : realName;
    }
}
