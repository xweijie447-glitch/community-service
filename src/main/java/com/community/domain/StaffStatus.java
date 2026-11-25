package com.community.domain;

/**
 * 服务人员接单状态
 */
public enum StaffStatus {

    FREE("可接单", "success"),
    BUSY("忙碌中", "warning"),
    OFF("休息中", "muted");

    private final String label;
    private final String color;

    StaffStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}
