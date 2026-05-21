package com.nippyclouding.tech_log_back.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.image")
public record ImageStorageProperties(
        String uploadDir,
        String publicPath,
        DataSize maxTotalSize
) {
}
