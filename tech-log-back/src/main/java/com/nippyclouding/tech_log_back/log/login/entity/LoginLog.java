package com.nippyclouding.tech_log_back.log.login.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "LOGIN_LOGS")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_log_id")
    private Long id;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "login_id", nullable = false, length = 100)
    private String loginId;

    @Column(name = "access_ip", nullable = false, length = 45)
    private String accessIp;

    @Builder
    public LoginLog(String provider, String loginId, String accessIp) {
        this.provider = provider;
        this.loginId = loginId;
        this.accessIp = accessIp;
    }

}
