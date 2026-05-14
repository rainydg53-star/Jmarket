package com.jmarket.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentApproveRequest(
        @NotBlank String orderId,
        @NotBlank String pgToken
) {
}
