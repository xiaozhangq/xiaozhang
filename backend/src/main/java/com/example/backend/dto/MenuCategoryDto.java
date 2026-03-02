package com.example.backend.dto;

import com.example.backend.domain.MenuCategory;

public record MenuCategoryDto(
        Long id,
        String name,
        Integer sortOrder,
        Boolean active
) {
    public static MenuCategoryDto from(MenuCategory category) {
        return new MenuCategoryDto(
                category.getId(),
                category.getName(),
                category.getSortOrder(),
                category.getActive()
        );
    }
}
