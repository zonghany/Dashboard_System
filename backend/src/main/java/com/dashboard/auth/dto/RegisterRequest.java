package com.dashboard.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式无效")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少需要8位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "密码必须包含大写字母、小写字母和数字")
    private String password;

    @NotBlank(message = "用户名不能为空")
    private String username;
}