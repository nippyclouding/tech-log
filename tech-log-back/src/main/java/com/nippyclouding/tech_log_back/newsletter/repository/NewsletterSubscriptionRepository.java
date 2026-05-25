package com.nippyclouding.tech_log_back.newsletter.repository;

import com.nippyclouding.tech_log_back.newsletter.entity.NewsletterSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long> {

    Optional<NewsletterSubscription> findByEmail(String email);

    Optional<NewsletterSubscription> findByConfirmationToken(String confirmationToken);

    Optional<NewsletterSubscription> findByUnsubscribeToken(String unsubscribeToken);

    List<NewsletterSubscription> findAllByConfirmedTrue();
}
