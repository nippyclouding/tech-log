package com.nippyclouding.tech_log_back.newsletter.event;

public record NewsletterConfirmationRequestedEvent(
        String email,
        String confirmationToken
) {
}
