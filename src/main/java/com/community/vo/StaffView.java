package com.community.vo;

import com.community.domain.StaffProfile;
import com.community.domain.StaffStatus;
import com.community.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务人员展示对象：账号信息 + 档案 + 当前在手单量
 */
@Getter
@AllArgsConstructor
public class StaffView {

    private final StaffProfile profile;
    private final User user;
    private final long activeOrders;

    public Long getUserId() {
        return user == null ? null : user.getId();
    }

    public String getRealName() {
        return user == null ? "-" : user.getDisplayName();
    }

    public String getPhone() {
        return user == null ? "-" : user.getPhone();
    }

    public String getStatusLabel() {
        return profile.getStatus().getLabel();
    }

    public String getStatusColor() {
        return profile.getStatus().getColor();
    }

    public boolean isFree() {
        return profile.getStatus() == StaffStatus.FREE;
    }

    /** 剩余可接单量 */
    public int getRemaining() {
        return Math.max(0, profile.getMaxDailyOrders() - (int) activeOrders);
    }
}
