package com.nippyclouding.tech_log_back.log.access.dto;

import com.nippyclouding.tech_log_back.log.access.entity.AccessLog;

public record AccessLogResponse(
        Long id,
        String ip,
        String path,
        String method,
        int statusCode,
        String timestamp,
        String requestId,
        String errorType,
        String errorMessage,
        String stackTrace
) {

    public static AccessLogResponse from(AccessLog accessLog) {
        return new AccessLogResponse(
                accessLog.getId(),
                accessLog.getAccessIp(),
                accessLog.getRequestUri(),
                accessLog.getMethod().name(),
                accessLog.getStatusCode(),
                accessLog.getUpdatedAt().toString(),
                accessLog.getRequestId(),
                accessLog.getErrorType(),
                accessLog.getErrorMessage(),
                accessLog.getStackTrace()
        );
    }
}
