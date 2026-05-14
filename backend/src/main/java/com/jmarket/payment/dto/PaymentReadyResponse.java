package com.jmarket.payment.dto;

public record PaymentReadyResponse(
        String orderId,
        String redirectUrl
) {
}
