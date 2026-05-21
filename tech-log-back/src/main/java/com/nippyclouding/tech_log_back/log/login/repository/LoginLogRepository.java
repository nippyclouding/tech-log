package com.nippyclouding.tech_log_back.log.login.repository;

import com.nippyclouding.tech_log_back.log.login.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    Page<LoginLog> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
