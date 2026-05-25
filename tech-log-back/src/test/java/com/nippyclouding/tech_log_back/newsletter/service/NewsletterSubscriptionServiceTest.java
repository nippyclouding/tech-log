package com.nippyclouding.tech_log_back.newsletter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nippyclouding.tech_log_back.newsletter.entity.NewsletterSubscription;
import com.nippyclouding.tech_log_back.newsletter.event.NewsletterConfirmationRequestedEvent;
import com.nippyclouding.tech_log_back.newsletter.event.NewsletterUnsubscribeRequestedEvent;
import com.nippyclouding.tech_log_back.newsletter.repository.NewsletterSubscriptionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NewsletterSubscriptionServiceTest {

    @Mock
    private NewsletterSubscriptionRepository subscriptionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NewsletterSubscriptionService subscriptionService;

    @Test
    void subscribe_savesNormalizedPendingSubscriptionAndRequestsConfirmation() {
        given(subscriptionRepository.findByEmail("reader@example.com")).willReturn(Optional.empty());
        given(subscriptionRepository.save(any(NewsletterSubscription.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.subscribe(" Reader@Example.com ");

        ArgumentCaptor<NewsletterSubscription> subscriptionCaptor = ArgumentCaptor.forClass(NewsletterSubscription.class);
        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue().getEmail()).isEqualTo("reader@example.com");
        assertThat(subscriptionCaptor.getValue().isConfirmed()).isFalse();
        verify(eventPublisher).publishEvent(any(NewsletterConfirmationRequestedEvent.class));
    }

    @Test
    void subscribe_doesNotResendConfirmationForConfirmedSubscriber() {
        NewsletterSubscription subscription = new NewsletterSubscription("reader@example.com");
        subscription.confirm();
        given(subscriptionRepository.findByEmail("reader@example.com")).willReturn(Optional.of(subscription));

        subscriptionService.subscribe("reader@example.com");

        verify(subscriptionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void confirm_activatesSubscriptionByToken() {
        NewsletterSubscription subscription = new NewsletterSubscription("reader@example.com");
        given(subscriptionRepository.findByConfirmationToken(subscription.getConfirmationToken()))
                .willReturn(Optional.of(subscription));

        boolean confirmed = subscriptionService.confirm(subscription.getConfirmationToken());

        assertThat(confirmed).isTrue();
        assertThat(subscription.isConfirmed()).isTrue();
    }

    @Test
    void requestUnsubscribe_sendsConfirmationEventInsteadOfDeletingByEmail() {
        NewsletterSubscription subscription = new NewsletterSubscription("reader@example.com");
        given(subscriptionRepository.findByEmail("reader@example.com")).willReturn(Optional.of(subscription));

        subscriptionService.requestUnsubscribe("reader@example.com");

        verify(eventPublisher).publishEvent(any(NewsletterUnsubscribeRequestedEvent.class));
        verify(subscriptionRepository, never()).delete(subscription);
    }

    @Test
    void unsubscribeByToken_deletesSubscription() {
        NewsletterSubscription subscription = new NewsletterSubscription("reader@example.com");
        given(subscriptionRepository.findByUnsubscribeToken(subscription.getUnsubscribeToken()))
                .willReturn(Optional.of(subscription));

        boolean unsubscribed = subscriptionService.unsubscribeByToken(subscription.getUnsubscribeToken());

        assertThat(unsubscribed).isTrue();
        verify(subscriptionRepository).delete(subscription);
    }
}
