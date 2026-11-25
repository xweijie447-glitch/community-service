package com.community.domain;

import com.community.storage.HasId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 服务分类：家政保洁、家电维修、快递代收、上门开锁 ...
 */
@Getter
@Setter
@NoArgsConstructor
public class ServiceCategory implements HasId {

    private Long id;

    private String code;

    private String name;

    /** 展示图标（emoji，不依赖第三方图标库） */
    private String icon;

    private String description;

    private Integer sortOrder = 0;

    private Boolean enabled = Boolean.TRUE;

    private LocalDateTime createdAt;

    public void touch() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
