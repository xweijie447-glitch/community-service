package com.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 服务评价表单
 */
@Getter
@Setter
public class ReviewForm {

    @NotNull(message = "订单标识缺失")
    private Long orderId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer score;

    /** 标签以逗号拼接提交，例如 准时,专业 */
    @Size(max = 100, message = "标签过长")
    private String tags;

    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;
}
