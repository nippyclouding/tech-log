package com.nippyclouding.tech_log_back.image.service;

public record StoredImage(
        String publicUrl,
        String originalName,
        String storedName,
        String contentType,
        long fileSize,
        int order,
        boolean thumbnail
) {
}
