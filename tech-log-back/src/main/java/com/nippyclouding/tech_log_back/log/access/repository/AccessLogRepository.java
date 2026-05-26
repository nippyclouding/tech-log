package com.nippyclouding.tech_log_back.log.access.repository;

import com.nippyclouding.tech_log_back.log.access.entity.AccessLog;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Page<AccessLog> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    long deleteByUpdatedAtBefore(LocalDateTime cutoff);
}
