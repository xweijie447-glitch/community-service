package com.community.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 智能派单候选：得分构成展示在管理端，便于人工复核
 */
@Getter
@AllArgsConstructor
public class DispatchCandidate {

    private final Long staffId;
    private final String staffName;
    private final double totalScore;
    private final double skillScore;
    private final double ratingScore;
    private final double loadScore;
    private final List<String> matchedSkills;
    private final long activeOrders;
    private final int remaining;
    private final String statusLabel;
    private final boolean available;
    private final String reason;

    public String getScoreText() {
        return String.format("%.1f", totalScore);
    }

    public boolean isSkillMatched() {
        return !matchedSkills.isEmpty();
    }
}
