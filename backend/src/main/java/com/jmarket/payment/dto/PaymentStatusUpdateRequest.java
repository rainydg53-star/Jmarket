package com.jmarket.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentStatusUpdateRequest(
        @NotBlank String orderId,
        String reason
) {
}
