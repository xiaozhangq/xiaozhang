package com.example.backend.dto;

public record CustomerProfileResponse(
        Long id,
        String username,
        String phone
) {
}
