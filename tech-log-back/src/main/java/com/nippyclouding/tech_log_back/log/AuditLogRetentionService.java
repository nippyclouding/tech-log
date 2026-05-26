package com.nippyclouding.tech_log_back.log;

import com.nippyclouding.tech_log_back.log.access.repository.AccessLogRepository;
import com.nippyclouding.tech_log_back.log.login.repository.LoginLogRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionService.class);

    private final AccessLogRepository accessLogRepository;
    private final LoginLogRepository loginLogRepository;

    @Value("${app.audit-log.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${app.audit-log.cleanup-cron:0 15 3 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredLogs() {
        int days = Math.max(retentionDays, 1);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        long accessDeleted = accessLogRepository.deleteByUpdatedAtBefore(cutoff);
        long loginDeleted = loginLogRepository.deleteByUpdatedAtBefore(cutoff);

        if (accessDeleted > 0 || loginDeleted > 0) {
            log.info(
                    "Deleted expired audit logs retentionDays={} accessLogs={} loginLogs={}",
                    days,
                    accessDeleted,
                    loginDeleted
            );
        }
    }
}
