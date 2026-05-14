package com.jmarket.common.exception;

public class JmarketException extends RuntimeException {

    private final ErrorCode errorCode;

    public JmarketException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public JmarketException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
