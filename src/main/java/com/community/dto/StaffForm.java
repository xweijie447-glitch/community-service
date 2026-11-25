package com.community.dto;

import com.community.domain.StaffStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

/**
 * 服务人员新增 / 编辑表单（账号 + 档案一起维护）
 */
@Getter
@Setter
public class StaffForm {

    /** 为空表示新增 */
    private Long userId;

    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "账号需为 4-20 位字母、数字或下划线")
    private String username;

    /** 新增必填；编辑留空表示不修改密码 */
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名不能超过 20 个字")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    private String email;

    @NotBlank(message = "请填写技能标签")
    @Size(max = 255, message = "技能标签过长")
    private String skills;

    @NotNull(message = "请设置上班时间")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime workStart = LocalTime.of(8, 0);

    @NotNull(message = "请设置下班时间")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime workEnd = LocalTime.of(18, 0);

    @NotNull(message = "请设置每日最大接单量")
    @Min(value = 1, message = "每日接单量至少 1 单")
    @Max(value = 20, message = "每日接单量不能超过 20 单")
    private Integer maxDailyOrders = 5;

    @Min(value = 0, message = "从业年限不能为负数")
    @Max(value = 50, message = "从业年限过大")
    private Integer serviceYears = 1;

    @Size(max = 255, message = "简介不能超过 255 个字")
    private String intro;

    private StaffStatus status = StaffStatus.FREE;
}
