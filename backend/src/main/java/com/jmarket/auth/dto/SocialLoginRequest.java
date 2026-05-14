package com.jmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank String code,
        String state
) {
}
