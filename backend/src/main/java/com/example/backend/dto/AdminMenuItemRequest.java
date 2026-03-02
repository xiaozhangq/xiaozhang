package com.example.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminMenuItemRequest(
        @NotNull(message = "分类不能为空")
        Long categoryId,

        @NotBlank(message = "菜品名称不能为空")
        String name,

        @Size(max = 500, message = "描述不能超过500字")
        String description,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于0")
        BigDecimal price,

        @Size(max = 1000, message = "图片地址不能超过1000字")
        String imageUrl,

        @NotNull(message = "上架状态不能为空")
        Boolean available
) {
}
