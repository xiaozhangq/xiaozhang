package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 32, message = "用户名2-32位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码6-64位")
        String password,

        @Size(max = 20, message = "手机号最多20位")
        String phone
) {
}
