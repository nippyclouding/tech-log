package com.nippyclouding.tech_log_back.log.access.dto;

import com.nippyclouding.tech_log_back.log.access.entity.AccessLog;

public record AccessLogResponse(
        Long id,
        String ip,
        String path,
        String method,
        int statusCode,
        String timestamp
) {

    public static AccessLogResponse from(AccessLog accessLog) {
        return new AccessLogResponse(
                accessLog.getId(),
                accessLog.getAccessIp(),
                accessLog.getRequestUri(),
                accessLog.getMethod().name(),
                accessLog.getStatusCode(),
                accessLog.getUpdatedAt().toString()
        );
    }
}
