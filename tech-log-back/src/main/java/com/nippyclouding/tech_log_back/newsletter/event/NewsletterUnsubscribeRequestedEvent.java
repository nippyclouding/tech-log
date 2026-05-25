package com.nippyclouding.tech_log_back.newsletter.event;

public record NewsletterUnsubscribeRequestedEvent(
        String email,
        String unsubscribeToken
) {
}
