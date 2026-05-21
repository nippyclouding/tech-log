package com.nippyclouding.tech_log_back.global.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        int status,
        String path,
        LocalDateTime timestamp,
        List<FieldErrorResponse> errors
) {

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                errorCode.getCode(),
                message,
                errorCode.getStatus().value(),
                path,
                LocalDateTime.now(),
                List.of()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path, List<FieldErrorResponse> errors) {
        return new ErrorResponse(
                errorCode.getCode(),
                message,
                errorCode.getStatus().value(),
                path,
                LocalDateTime.now(),
                errors
        );
    }

    public record FieldErrorResponse(String field, String message) {
    }
}
