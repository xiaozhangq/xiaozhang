package com.example.backend.dto;

import java.util.List;

public record AdminLoginResponse(
        String token,
        String tokenType,
        long expiresAt,
        String username,
        List<String> roles
) {
}
