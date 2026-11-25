package com.community.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 预约下单表单
 */
@Getter
@Setter
public class OrderCreateForm {

    @NotNull(message = "请选择服务项目")
    private Long itemId;

    @NotNull(message = "请选择上门时间")
    @Future(message = "预约时间必须晚于当前时间")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime appointmentTime;

    @NotBlank(message = "服务地址不能为空")
    @Size(max = 200, message = "地址不能超过 200 个字")
    private String address;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 20, message = "联系人不能超过 20 个字")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String contactPhone;

    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 10, message = "单次预约数量不能超过 10")
    private Integer quantity = 1;

    @Size(max = 300, message = "备注不能超过 300 个字")
    private String remark;

    /** 幂等令牌，页面渲染时生成，重复提交复用同一张单 */
    @NotBlank(message = "请求令牌缺失，请刷新页面重试")
    private String requestToken;
}
