package com.jmarket.common.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.code(), errorCode.message(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        String responseMessage = message == null || message.isBlank() ? errorCode.message() : message;
        return new ErrorResponse(errorCode.code(), responseMessage, LocalDateTime.now());
    }
}
