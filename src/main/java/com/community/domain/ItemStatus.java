package com.community.domain;

/**
 * 服务项目上下架状态
 */
public enum ItemStatus {

    ON_SALE("已上架", "success"),
    OFF_SALE("已下架", "muted");

    private final String label;
    private final String color;

    ItemStatus(String label, String color) {
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
