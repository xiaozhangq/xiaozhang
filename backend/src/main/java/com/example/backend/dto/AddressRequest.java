package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "收货人不能为空")
        @Size(max = 32)
        String receiverName,

        @NotBlank(message = "电话不能为空")
        @Size(max = 20)
        String phone,

        @NotBlank(message = "地址不能为空")
        @Size(max = 256)
        String address,

        boolean defaultAddress
) {
}
