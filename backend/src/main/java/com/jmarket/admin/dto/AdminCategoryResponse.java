package com.jmarket.admin.dto;

import com.jmarket.admin.domain.AdminCategory;
import java.io.Serializable;
import java.time.LocalDateTime;

public record AdminCategoryResponse(
        Long id,
        String code,
        String name,
        int displayOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {
    public static AdminCategoryResponse from(AdminCategory category) {
        return new AdminCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
