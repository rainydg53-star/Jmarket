package com.jmarket.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCategoryUpdateRequest(
        @NotBlank String name,
        int displayOrder,
        boolean active
) {
}
