package com.nippyclouding.tech_log_back.log.access.controller;

import com.nippyclouding.tech_log_back.log.access.service.AccessLogService;
import com.nippyclouding.tech_log_back.log.access.dto.AccessLogResponse;
import com.nippyclouding.tech_log_back.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/access-logs")
public class AdminAccessLogController {

    private final AccessLogService accessLogService;

    @GetMapping
    public ResponseEntity<PageResponse<AccessLogResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(accessLogService.findAll(page, size));
    }
}
