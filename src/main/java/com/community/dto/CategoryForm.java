package com.community.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 服务分类表单
 */
@Getter
@Setter
public class CategoryForm {

    private Long id;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 50, message = "编码不能超过 50 个字符")
    private String code;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "名称不能超过 50 个字")
    private String name;

    @Size(max = 20, message = "图标过长")
    private String icon;

    @Size(max = 500, message = "描述不能超过 500 个字")
    private String description;

    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder = 0;

    private Boolean enabled = Boolean.TRUE;
}
