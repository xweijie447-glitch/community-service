package com.community.domain;

import com.community.storage.HasId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 派单记录：智能派单（AUTO）与人工改派（MANUAL）都留痕
 */
@Getter
@Setter
@NoArgsConstructor
public class DispatchRecord implements HasId {

    private Long id;

    private Long orderId;

    private Long staffId;

    private String staffName;

    /** AUTO / MANUAL */
    private String type;

    /** 匹配得分，人工派单为 0 */
    private Double matchScore;

    private String reason;

    private LocalDateTime createdAt;
}
