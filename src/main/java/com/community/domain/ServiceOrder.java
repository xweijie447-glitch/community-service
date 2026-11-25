package com.community.domain;

import com.community.storage.HasId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约单（订单）
 * <p>下单时对服务项目做快照（名称、价格、分类），避免后续改价影响历史订单。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ServiceOrder implements HasId {

    private Long id;

    private String orderNo;

    /** 幂等令牌：同一个表单重复提交只生成一张单 */
    private String requestToken;

    private Long residentId;

    /** 服务人员账号 id */
    private Long staffId;

    private Long serviceItemId;

    private String serviceItemName;

    private String categoryName;

    private BigDecimal price;

    private Integer quantity = 1;

    private BigDecimal totalAmount;

    private LocalDateTime appointmentTime;

    private String address;

    private String contactName;

    private String contactPhone;

    private String remark;

    private OrderStatus status = OrderStatus.PENDING;

    private String cancelReason;

    /** 是否已归档（清理任务标记，不物理删除，保留审计） */
    private Boolean archived = Boolean.FALSE;

    private Boolean reviewed = Boolean.FALSE;

    /** 状态变更次数，用于检测并发覆盖 */
    private Long version = 0L;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime assignedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    public void touch() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
}
