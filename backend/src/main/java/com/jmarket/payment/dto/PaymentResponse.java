package com.jmarket.payment.dto;

import com.jmarket.payment.domain.Payment;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String provider,
        String orderId,
        Long amount,
        String status,
        String redirectUrl,
        String failReason,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime failedAt,
        LocalDateTime canceledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getProvider(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getRedirectUrl(),
                payment.getFailReason(),
                payment.getCreatedAt(),
                payment.getApprovedAt(),
                payment.getFailedAt(),
                payment.getCanceledAt()
        );
    }
}
