package com.jmarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String newPassword,
        @NotBlank @Size(min = 8, max = 100) String newPasswordConfirm,
        @NotBlank String emailVerificationToken
) {
}
