package com.example.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotNull(message = "菜品ID不能为空")
        Long menuItemId,

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        Integer quantity
) {
}
