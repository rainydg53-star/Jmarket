package com.jmarket.mileage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MileageWithdrawalRejectRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
