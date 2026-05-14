package com.jmarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordVerifyRequest(
        @NotBlank @Email String email
) {
}
