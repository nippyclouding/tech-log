package com.nippyclouding.tech_log_back.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailNotificationProperties(
        boolean enabled,
        String from,
        String siteName
) {
}
