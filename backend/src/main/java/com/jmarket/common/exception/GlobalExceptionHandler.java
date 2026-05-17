package com.jmarket.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JmarketException.class)
    public ResponseEntity<?> handleJmarketException(JmarketException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        if (isSseRequest(request)) {
            return ResponseEntity.status(errorCode.httpStatus()).build();
        }
        return ResponseEntity.status(errorCode.httpStatus()).body(ErrorResponse.of(errorCode, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.INVALID_INPUT.code(), message, java.time.LocalDateTime.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.INVALID_INPUT.code(), ex.getMessage(), java.time.LocalDateTime.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.INVALID_INPUT.code(), ErrorCode.INVALID_INPUT.message(), java.time.LocalDateTime.now()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorCode.INVALID_INPUT.code(),
                        "\uC774\uBBF8\uC9C0\uB294 \uD30C\uC77C\uB2F9 5MB, \uD55C \uBC88\uC5D0 50MB \uC774\uD558\uB85C \uC5C5\uB85C\uB4DC\uD574 \uC8FC\uC138\uC694.",
                        java.time.LocalDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex, HttpServletRequest request) {
        if (isSseRequest(request)) {
            return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus()).build();
        }
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return (accept != null && accept.contains("text/event-stream"))
                || request.getRequestURI().startsWith("/api/notifications/stream");
    }
}
