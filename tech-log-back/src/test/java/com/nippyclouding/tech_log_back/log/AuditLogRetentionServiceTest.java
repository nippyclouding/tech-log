package com.nippyclouding.tech_log_back.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.nippyclouding.tech_log_back.log.access.repository.AccessLogRepository;
import com.nippyclouding.tech_log_back.log.login.repository.LoginLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionServiceTest {

    @Mock
    private AccessLogRepository accessLogRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    @Test
    void deletesAuditLogsOlderThanConfiguredRetention() {
        AuditLogRetentionService service = new AuditLogRetentionService(accessLogRepository, loginLogRepository);
        ReflectionTestUtils.setField(service, "retentionDays", 90);
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(90);

        service.deleteExpiredLogs();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(accessLogRepository).deleteByUpdatedAtBefore(cutoff.capture());
        verify(loginLogRepository).deleteByUpdatedAtBefore(any(LocalDateTime.class));
        assertThat(cutoff.getValue()).isBetween(expectedCutoff.minusSeconds(1), expectedCutoff.plusSeconds(1));
    }
}
