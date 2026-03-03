package com.example.backend.dto;

public record CustomerLoginResponse(
        String token,
        String tokenType,
        long expiresAt,
        String username
) {
}
