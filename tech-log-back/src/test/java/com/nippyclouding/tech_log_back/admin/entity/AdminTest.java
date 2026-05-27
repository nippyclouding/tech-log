package com.nippyclouding.tech_log_back.admin.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdminTest {

    @Test
    void locksAfterFiveFailedLoginsWithoutExtendingAnActiveLock() {
        Admin admin = new Admin("admin", "hash", "Admin");

        for (int attempt = 0; attempt < Admin.MAX_FAILED_LOGIN_ATTEMPTS; attempt++) {
            admin.recordFailedLogin();
        }
        var lockedUntil = admin.getLockedUntil();

        assertTrue(admin.isLocked());
        assertEquals(Admin.MAX_FAILED_LOGIN_ATTEMPTS, admin.getFailedLoginAttempts());
        assertNotNull(lockedUntil);

        admin.recordFailedLogin();

        assertEquals(lockedUntil, admin.getLockedUntil());
        assertEquals(Admin.MAX_FAILED_LOGIN_ATTEMPTS, admin.getFailedLoginAttempts());
    }

    @Test
    void successfulLoginResetsFailuresAndRecordsLoginTime() {
        Admin admin = new Admin("admin", "hash", "Admin");
        admin.recordFailedLogin();

        admin.recordSuccessfulLogin();

        assertEquals(0, admin.getFailedLoginAttempts());
        assertFalse(admin.isLocked());
        assertNotNull(admin.getLastLoginAt());
    }
}
