package com.jmarket.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentReadyRequest(
        @NotNull @Min(1) Long amount
) {
}
