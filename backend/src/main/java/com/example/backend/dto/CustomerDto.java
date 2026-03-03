package com.example.backend.dto;

import java.time.LocalDateTime;

import com.example.backend.domain.CustomerStatus;
import com.example.backend.domain.CustomerUser;

public record CustomerDto(
        Long id,
        String username,
        String phone,
        CustomerStatus status,
        LocalDateTime createdAt
) {
    public static CustomerDto from(CustomerUser u) {
        return new CustomerDto(
                u.getId(),
                u.getUsername(),
                u.getPhone(),
                u.getStatus(),
                u.getCreatedAt()
        );
    }
}
