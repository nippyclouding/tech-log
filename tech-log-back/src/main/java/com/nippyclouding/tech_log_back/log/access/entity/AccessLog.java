package com.nippyclouding.tech_log_back.log.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ACCESS_LOGS")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_log_id")
    private Long id;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "access_ip", nullable = false, length = 45)
    private String accessIp;

    @Column(name = "request_uri", nullable = false, length = 500)
    private String requestUri;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HttpMethodType method;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Builder
    public AccessLog(String accessIp, String requestUri, HttpMethodType method, int statusCode) {
        this.accessIp = accessIp;
        this.requestUri = requestUri;
        this.method = method;
        this.statusCode = statusCode;
    }

}
