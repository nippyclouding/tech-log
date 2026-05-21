package com.nippyclouding.tech_log_back.log.access.service;

import com.nippyclouding.tech_log_back.log.access.entity.AccessLog;
import com.nippyclouding.tech_log_back.log.access.repository.AccessLogRepository;
import com.nippyclouding.tech_log_back.log.access.entity.HttpMethodType;
import com.nippyclouding.tech_log_back.log.access.dto.AccessLogResponse;
import com.nippyclouding.tech_log_back.global.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    public PageResponse<AccessLogResponse> findAll(int page, int size) {
        return PageResponse.from(accessLogRepository.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, size)).map(AccessLogResponse::from));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String accessIp, String requestUri, String method, int statusCode) {
        accessLogRepository.save(new AccessLog(accessIp, requestUri, HttpMethodType.valueOf(method), statusCode));
    }
}
