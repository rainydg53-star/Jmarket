package com.jmarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record AdminRestrictionCreateRequest(
        @NotBlank String type,
        LocalDateTime restrictedUntil,
        String reason
) {
}
