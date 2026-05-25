package com.nippyclouding.tech_log_back.newsletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "NEWSLETTER_SUBSCRIPTIONS")
public class NewsletterSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "confirmation_token", nullable = false, unique = true, length = 36)
    private String confirmationToken;

    @Column(name = "unsubscribe_token", nullable = false, unique = true, length = 36)
    private String unsubscribeToken;

    @Column(name = "is_confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public NewsletterSubscription(String email) {
        this.email = normalizeEmail(email);
        renewTokens();
    }

    public void requestConfirmation() {
        confirmationToken = UUID.randomUUID().toString();
        updatedAt = LocalDateTime.now();
    }

    public void confirm() {
        confirmed = true;
        updatedAt = LocalDateTime.now();
    }

    private void renewTokens() {
        confirmationToken = UUID.randomUUID().toString();
        unsubscribeToken = UUID.randomUUID().toString();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
