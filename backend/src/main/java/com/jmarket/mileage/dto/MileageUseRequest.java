package com.jmarket.mileage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MileageUseRequest(
        @NotNull @Min(1) Long amount
) {
}
