package com.community.domain;

import com.community.storage.HasId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 服务人员档案：技能标签、服务时段、接单状态、绩效
 */
@Getter
@Setter
@NoArgsConstructor
public class StaffProfile implements HasId {

    private Long id;

    /** 对应账号 id */
    private Long userId;

    /** 技能标签，逗号分隔，例如：保洁,深度清洁,玻璃清洁 */
    private String skills;

    private LocalTime workStart = LocalTime.of(8, 0);

    private LocalTime workEnd = LocalTime.of(18, 0);

    /** 每日最大接单量 */
    private Integer maxDailyOrders = 5;

    private StaffStatus status = StaffStatus.FREE;

    private Integer serviceYears = 1;

    private String intro;

    private Double avgScore = 5.0;

    private Integer completedCount = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void touch() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @JsonIgnore
    public List<String> getSkillList() {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skills.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @JsonIgnore
    public Set<String> skillSet() {
        return new LinkedHashSet<>(getSkillList());
    }
}
