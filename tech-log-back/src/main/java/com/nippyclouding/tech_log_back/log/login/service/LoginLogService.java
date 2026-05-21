package com.nippyclouding.tech_log_back.log.login.service;

import com.nippyclouding.tech_log_back.log.login.entity.LoginLog;
import com.nippyclouding.tech_log_back.log.login.repository.LoginLogRepository;
import com.nippyclouding.tech_log_back.global.dto.PageResponse;
import com.nippyclouding.tech_log_back.log.login.dto.LoginLogResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public PageResponse<LoginLogResponse> findAll(int page, int size) {
        return PageResponse.from(loginLogRepository.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, size)).map(LoginLogResponse::from));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String provider, String loginId, String accessIp) {
        loginLogRepository.save(new LoginLog(provider, loginId, accessIp));
    }
}
