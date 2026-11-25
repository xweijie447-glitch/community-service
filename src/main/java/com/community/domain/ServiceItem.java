package com.community.domain;

import com.community.storage.HasId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务项目（具体可预约的服务）
 */
@Getter
@Setter
@NoArgsConstructor
public class ServiceItem implements HasId {

    private Long id;

    private Long categoryId;

    private String name;

    /** 一句话卖点 */
    private String subtitle;

    /** 价格与服务说明 */
    private String description;

    private BigDecimal price = BigDecimal.ZERO;

    /** 计价单位：次 / 小时 / 台 */
    private String unit = "次";

    /** 预计服务时长（分钟），用于派单时间冲突判断 */
    private Integer durationMinutes = 60;

    private Boolean hot = Boolean.FALSE;

    private ItemStatus status = ItemStatus.ON_SALE;

    /** 累计完成单量，热门排序依据之一 */
    private Integer sales = 0;

    private Integer reviewCount = 0;

    private Integer scoreTotal = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void touch() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    /** 平均评分，保留一位小数 */
    @JsonIgnore
    public double getAvgScore() {
        if (reviewCount == null || reviewCount == 0) {
            return 5.0;
        }
        return Math.round(scoreTotal * 10.0 / reviewCount) / 10.0;
    }

    @JsonIgnore
    public boolean isOnSale() {
        return status == ItemStatus.ON_SALE;
    }
}
