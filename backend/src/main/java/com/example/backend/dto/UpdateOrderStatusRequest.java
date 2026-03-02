package com.example.backend.dto;

import com.example.backend.domain.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "订单状态不能为空")
        OrderStatus status
) {
}
