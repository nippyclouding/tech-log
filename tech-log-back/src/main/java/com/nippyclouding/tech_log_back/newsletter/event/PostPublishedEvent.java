package com.nippyclouding.tech_log_back.newsletter.event;

public record PostPublishedEvent(
        Long postId,
        String title
) {
}
