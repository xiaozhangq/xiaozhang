package com.example.backend.dto;

import com.example.backend.domain.CustomerStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateCustomerStatusRequest(
        @NotNull(message = "状态不能为空")
        CustomerStatus status
) {
}
