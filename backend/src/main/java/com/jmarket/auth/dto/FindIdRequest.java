package com.jmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FindIdRequest(
        @NotBlank @Size(min = 2, max = 30) String name
) {
}
