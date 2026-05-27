package com.nippyclouding.tech_log_back.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ADMINS")
public class Admin {

    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long id;

    @Column(name = "username", nullable = false, length = 100, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "role", nullable = false, length = 30)
    private String role = "ADMIN";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at", nullable = false)
    private LocalDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Admin(String username, String passwordHash, String displayName) {
        LocalDateTime now = LocalDateTime.now();
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.passwordChangedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void recordSuccessfulLogin() {
        LocalDateTime now = LocalDateTime.now();
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
        updatedAt = now;
    }

    public void recordFailedLogin() {
        LocalDateTime now = LocalDateTime.now();
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return;
        }
        if (lockedUntil != null) {
            failedLoginAttempts = 0;
            lockedUntil = null;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            lockedUntil = now.plusMinutes(LOCK_MINUTES);
        }
        updatedAt = now;
    }
}
