package com.nippyclouding.tech_log_back.log.login.dto;

import com.nippyclouding.tech_log_back.log.login.entity.LoginLog;

public record LoginLogResponse(
        Long id,
        String provider,
        String loginId,
        String ip,
        String timestamp
) {

    public static LoginLogResponse from(LoginLog loginLog) {
        return new LoginLogResponse(
                loginLog.getId(),
                loginLog.getProvider(),
                loginLog.getLoginId(),
                loginLog.getAccessIp(),
                loginLog.getUpdatedAt().toString()
        );
    }
}
