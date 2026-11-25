package com.community.domain;

import com.community.storage.HasId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 服务评价，一单一评
 */
@Getter
@Setter
@NoArgsConstructor
public class Review implements HasId {

    private Long id;

    private Long orderId;

    private String orderNo;

    private Long residentId;

    private Long staffId;

    private Long itemId;

    /** 1~5 分 */
    private Integer score;

    /** 标签，逗号分隔：准时,专业,态度好 */
    private String tags;

    private String content;

    private LocalDateTime createdAt;
}
