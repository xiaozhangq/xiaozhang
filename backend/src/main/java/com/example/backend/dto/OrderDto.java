package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.OrderStatus;

public record OrderDto(
        Long id,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        String remark,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemDto> items
) {
    public static OrderDto from(CustomerOrder order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(OrderItemDto::from)
                .toList();
        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryAddress(),
                order.getRemark(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemDtos
        );
    }
}
