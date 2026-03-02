package com.example.backend.dto;

import java.math.BigDecimal;

public record CreateOrderResponse(
        Long orderId,
        BigDecimal totalAmount,
        String message
) {
}
