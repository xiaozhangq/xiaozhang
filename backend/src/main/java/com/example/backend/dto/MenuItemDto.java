package com.example.backend.dto;

import java.math.BigDecimal;

import com.example.backend.domain.MenuItem;

public record MenuItemDto(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Boolean available
) {
    public static MenuItemDto from(MenuItem item) {
        return new MenuItemDto(
                item.getId(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.getAvailable()
        );
    }
}
