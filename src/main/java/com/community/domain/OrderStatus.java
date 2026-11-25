package com.community.domain;

import java.util.Arrays;
import java.util.List;

/**
 * 订单状态流转：
 * <pre>
 * 待接单 PENDING ──派单──> 已派单 ASSIGNED ──开始服务──> 服务中 IN_SERVICE ──完成──> 已完成 COMPLETED
 *     │                        │                          │
 *     └──────取消──────────────┴──────────取消────────────┘──> 已取消 CANCELLED
 * </pre>
 * 状态机集中在此，避免业务代码里散落 if-else，同时为幂等控制提供依据。
 */
public enum OrderStatus {

    PENDING("待接单", "warning"),
    ASSIGNED("已派单", "info"),
    IN_SERVICE("服务中", "primary"),
    COMPLETED("已完成", "success"),
    CANCELLED("已取消", "muted");

    private final String label;
    private final String color;

    OrderStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    /** 当前状态允许流转到的下一批状态 */
    public List<OrderStatus> nextStates() {
        return switch (this) {
            case PENDING -> Arrays.asList(ASSIGNED, CANCELLED);
            case ASSIGNED -> Arrays.asList(IN_SERVICE, CANCELLED);
            case IN_SERVICE -> Arrays.asList(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> List.of();
        };
    }

    public boolean canTransitTo(OrderStatus target) {
        return nextStates().contains(target);
    }

    /** 是否属于终态（不可再流转），用于幂等判断 */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** 是否仍占用服务人员产能 */
    public boolean isActive() {
        return this == PENDING || this == ASSIGNED || this == IN_SERVICE;
    }
}
