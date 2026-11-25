package com.community.mq.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 评价统计消息：异步汇总服务人员评分与服务项目评分
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsMessage {

    private Long reviewId;
    private Long staffId;
    private Long itemId;
    private Integer score;
    private String orderNo;
}
