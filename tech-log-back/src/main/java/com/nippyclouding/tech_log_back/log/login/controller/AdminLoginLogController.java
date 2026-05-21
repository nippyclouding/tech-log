package com.nippyclouding.tech_log_back.log.login.controller;

import com.nippyclouding.tech_log_back.log.login.service.LoginLogService;
import com.nippyclouding.tech_log_back.global.dto.PageResponse;
import com.nippyclouding.tech_log_back.log.login.dto.LoginLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/login-logs")
public class AdminLoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    public ResponseEntity<PageResponse<LoginLogResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(loginLogService.findAll(page, size));
    }
}
