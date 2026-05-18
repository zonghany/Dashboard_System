package com.dashboard.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式无效")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;
}