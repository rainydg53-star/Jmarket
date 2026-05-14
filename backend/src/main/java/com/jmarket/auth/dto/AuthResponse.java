package com.jmarket.auth.dto;

public record AuthResponse(
        String accessToken,
        UserMeResponse user
) {
}
