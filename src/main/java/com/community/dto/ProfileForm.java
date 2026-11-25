package com.community.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 居民个人资料表单
 */
@Getter
@Setter
public class ProfileForm {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名不能超过 20 个字")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 200, message = "地址不能超过 200 个字")
    private String address;

    public static ProfileForm from(com.community.domain.User user) {
        ProfileForm form = new ProfileForm();
        form.setRealName(user.getRealName());
        form.setPhone(user.getPhone());
        form.setEmail(user.getEmail());
        form.setAddress(user.getAddress());
        return form;
    }
}
