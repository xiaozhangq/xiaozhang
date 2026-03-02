package com.example.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank(message = "联系人不能为空")
        String customerName,

        @NotBlank(message = "联系电话不能为空")
        String customerPhone,

        @NotBlank(message = "配送地址不能为空")
        String deliveryAddress,

        @Size(max = 500, message = "备注不能超过500字")
        String remark,

        @NotEmpty(message = "订单至少包含一个菜品")
        List<@Valid CreateOrderItemRequest> items
) {
}
