package com.example.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        String name,

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,

        @NotNull(message = "状态不能为空")
        Boolean active
) {
}
