package com.example.backend.dto;

import java.math.BigDecimal;

import com.example.backend.domain.CustomerOrderItem;

public record OrderItemDto(
        Long id,
        Long menuItemId,
        String menuItemName,
        BigDecimal menuItemPrice,
        Integer quantity,
        BigDecimal subtotal
) {
    public static OrderItemDto from(CustomerOrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getMenuItemId(),
                item.getMenuItemName(),
                item.getMenuItemPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
