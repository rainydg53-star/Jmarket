package com.jmarket.auth.dto;

public record EmailVerificationSendResponse(
        String message,
        Integer expiresInSeconds,
        String devCode
) {
}
