package com.jmarket.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCategoryRequest(
        @NotBlank String code,
        @NotBlank String name,
        int displayOrder,
        boolean active
) {
}
