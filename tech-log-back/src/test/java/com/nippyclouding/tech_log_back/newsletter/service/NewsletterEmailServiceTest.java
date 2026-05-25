package com.nippyclouding.tech_log_back.newsletter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nippyclouding.tech_log_back.global.config.MailNotificationProperties;
import com.nippyclouding.tech_log_back.newsletter.entity.NewsletterSubscription;
import com.nippyclouding.tech_log_back.newsletter.event.PostPublishedEvent;
import com.nippyclouding.tech_log_back.newsletter.repository.NewsletterSubscriptionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NewsletterEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NewsletterSubscriptionRepository subscriptionRepository;

    @Test
    void sendPostPublishedMail_sendsOnlyRepositoryConfirmedRecipientsWithUnsubscribeLink() {
        NewsletterSubscription subscription = new NewsletterSubscription("reader@example.com");
        subscription.confirm();
        given(subscriptionRepository.findAllByConfirmedTrue()).willReturn(List.of(subscription));
        NewsletterEmailService service = new NewsletterEmailService(
                mailSender,
                subscriptionRepository,
                new MailNotificationProperties(true, "sender@example.com", "Tech Log")
        );
        ReflectionTestUtils.setField(service, "frontendOrigin", "https://blog.example.com");

        service.sendPostPublishedMail(new PostPublishedEvent(3L, "새 글"));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("reader@example.com");
        assertThat(message.getText())
                .contains("https://blog.example.com/post/3")
                .contains("/api/subscriptions/unsubscribe?token=" + subscription.getUnsubscribeToken());
    }
}
