package com.nippyclouding.tech_log_back.newsletter.service;

import com.nippyclouding.tech_log_back.newsletter.entity.NewsletterSubscription;
import com.nippyclouding.tech_log_back.newsletter.event.NewsletterConfirmationRequestedEvent;
import com.nippyclouding.tech_log_back.newsletter.event.NewsletterUnsubscribeRequestedEvent;
import com.nippyclouding.tech_log_back.newsletter.repository.NewsletterSubscriptionRepository;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterSubscriptionService {

    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void subscribe(String email) {
        String normalizedEmail = normalizeEmail(email);
        NewsletterSubscription subscription = subscriptionRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> subscriptionRepository.save(new NewsletterSubscription(normalizedEmail)));
        if (subscription.isConfirmed()) {
            return;
        }
        subscription.requestConfirmation();
        eventPublisher.publishEvent(new NewsletterConfirmationRequestedEvent(
                subscription.getEmail(),
                subscription.getConfirmationToken()
        ));
    }

    @Transactional
    public boolean confirm(String token) {
        return subscriptionRepository.findByConfirmationToken(token)
                .map(subscription -> {
                    subscription.confirm();
                    return true;
                })
                .orElse(false);
    }

    public void requestUnsubscribe(String email) {
        subscriptionRepository.findByEmail(normalizeEmail(email)).ifPresent(subscription ->
                eventPublisher.publishEvent(new NewsletterUnsubscribeRequestedEvent(
                        subscription.getEmail(),
                        subscription.getUnsubscribeToken()
                )));
    }

    @Transactional
    public boolean unsubscribeByToken(String token) {
        return subscriptionRepository.findByUnsubscribeToken(token)
                .map(subscription -> {
                    subscriptionRepository.delete(subscription);
                    return true;
                })
                .orElse(false);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
