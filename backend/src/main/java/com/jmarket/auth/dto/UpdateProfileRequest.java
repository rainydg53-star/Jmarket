package com.jmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 30) String name,
        @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[a-zA-Z0-9가-힣_]+$") String nickname,
        @NotBlank @Pattern(regexp = "^[0-9]+$") @Size(min = 10, max = 11) String phoneNumber
) {
}
