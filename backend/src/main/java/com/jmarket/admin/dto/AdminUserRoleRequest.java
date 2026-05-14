package com.jmarket.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserRoleRequest(
        @NotBlank String role
) {
}
