package com.jmarket.mileage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MileageAdminAdjustmentRequest(
        @NotNull MileageAdminAdjustmentType type,
        @NotNull @Min(1) Long amount,
        @NotBlank @Size(max = 500) String reason
) {
}
