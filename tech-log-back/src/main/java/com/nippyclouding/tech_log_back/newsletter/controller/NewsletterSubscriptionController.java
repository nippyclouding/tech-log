package com.nippyclouding.tech_log_back.newsletter.controller;

import com.nippyclouding.tech_log_back.newsletter.dto.NewsletterSubscriptionRequest;
import com.nippyclouding.tech_log_back.newsletter.dto.NewsletterSubscriptionResponse;
import com.nippyclouding.tech_log_back.newsletter.service.NewsletterSubscriptionService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class NewsletterSubscriptionController {

    private final NewsletterSubscriptionService subscriptionService;

    @Value("${app.frontend-origin:http://localhost:3000}")
    private String frontendOrigin;

    @PostMapping
    public ResponseEntity<NewsletterSubscriptionResponse> subscribe(
            @Valid @RequestBody NewsletterSubscriptionRequest request
    ) {
        subscriptionService.subscribe(request.email());
        return ResponseEntity.accepted().body(
                new NewsletterSubscriptionResponse("입력한 이메일에서 구독 확인 링크를 확인해주세요.")
        );
    }

    @PostMapping("/unsubscribe-request")
    public ResponseEntity<NewsletterSubscriptionResponse> requestUnsubscribe(
            @Valid @RequestBody NewsletterSubscriptionRequest request
    ) {
        subscriptionService.requestUnsubscribe(request.email());
        return ResponseEntity.accepted().body(
                new NewsletterSubscriptionResponse("입력한 이메일에서 구독 취소 링크를 확인해주세요.")
        );
    }

    @GetMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestParam String token) {
        boolean confirmed = subscriptionService.confirm(token);
        return redirect(confirmed ? "confirmed" : "invalid");
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam String token) {
        boolean unsubscribed = subscriptionService.unsubscribeByToken(token);
        return redirect(unsubscribed ? "unsubscribed" : "invalid");
    }

    private ResponseEntity<Void> redirect(String status) {
        URI location = UriComponentsBuilder.fromUriString(frontendOrigin)
                .queryParam("subscription", status)
                .build()
                .toUri();
        return ResponseEntity.status(302).location(location).build();
    }
}
