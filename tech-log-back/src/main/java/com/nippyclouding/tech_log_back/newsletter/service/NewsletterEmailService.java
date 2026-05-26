package com.nippyclouding.tech_log_back.newsletter.service;

import com.nippyclouding.tech_log_back.global.config.MailNotificationProperties;
import com.nippyclouding.tech_log_back.newsletter.entity.NewsletterSubscription;
import com.nippyclouding.tech_log_back.newsletter.event.NewsletterConfirmationRequestedEvent;
import com.nippyclouding.tech_log_back.newsletter.event.NewsletterUnsubscribeRequestedEvent;
import com.nippyclouding.tech_log_back.newsletter.event.PostPublishedEvent;
import com.nippyclouding.tech_log_back.newsletter.repository.NewsletterSubscriptionRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsletterEmailService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterEmailService.class);

    private final JavaMailSender mailSender;
    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final MailNotificationProperties properties;

    @Value("${app.frontend-origin:http://localhost:3000}")
    private String frontendOrigin;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendConfirmationMail(NewsletterConfirmationRequestedEvent event) {
        if (!canSend()) {
            return;
        }
        String confirmationUrl = link("/api/subscriptions/confirm", event.confirmationToken());
        send(
                event.email(),
                "[" + siteName() + "] 이메일 구독을 확인해주세요",
                "아래 링크를 열면 새 게시글 알림 구독이 완료됩니다.\n\n"
                        + confirmationUrl + "\n\n신청하지 않았다면 이 메일을 무시하세요."
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendUnsubscribeMail(NewsletterUnsubscribeRequestedEvent event) {
        if (!canSend()) {
            return;
        }
        String unsubscribeUrl = link("/api/subscriptions/unsubscribe", event.unsubscribeToken());
        send(
                event.email(),
                "[" + siteName() + "] 이메일 구독 취소 확인",
                "아래 링크를 열면 새 게시글 알림 구독이 취소됩니다.\n\n"
                        + unsubscribeUrl + "\n\n요청하지 않았다면 이 메일을 무시하세요."
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendPostPublishedMail(PostPublishedEvent event) {
        if (!canSend()) {
            return;
        }
        String postUrl = frontendOrigin + "/post/" + event.postId();
        subscriptionRepository.findAllByConfirmedTrue().forEach(subscription ->
                sendPostPublishedMail(subscription, event.title(), postUrl));
    }

    private void sendPostPublishedMail(NewsletterSubscription subscription, String title, String postUrl) {
        String unsubscribeUrl = link("/api/subscriptions/unsubscribe", subscription.getUnsubscribeToken());
        send(
                subscription.getEmail(),
                "[" + siteName() + "] 새 게시글: " + title,
                "새 게시글이 발행되었습니다.\n\n"
                        + title + "\n"
                        + postUrl + "\n\n"
                        + "구독 취소: " + unsubscribeUrl
        );
    }

    private void send(String recipient, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.warn("Newsletter email delivery failed for {}", maskEmail(recipient), e);
        }
    }

    private boolean canSend() {
        if (!properties.enabled()) {
            return false;
        }
        if (properties.from() == null || properties.from().isBlank()) {
            log.warn("Newsletter mail is enabled but app.mail.from is empty.");
            return false;
        }
        return true;
    }

    private String siteName() {
        return properties.siteName() == null || properties.siteName().isBlank()
                ? "Tech Log"
                : properties.siteName();
    }

    private String link(String path, String token) {
        return frontendOrigin + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        int separator = email.indexOf('@');
        if (separator <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(separator);
    }
}
