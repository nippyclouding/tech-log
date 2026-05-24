package com.nippyclouding.tech_log_back.global.web;

import com.nippyclouding.tech_log_back.log.access.service.AccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Set<String> RECORDABLE_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> MUTATION_METHODS = Set.of("POST", "PUT", "DELETE");
    private static final Set<String> EXCLUDED_PATHS = Set.of("/api/admin/access-logs", "/api/admin/login-logs");

    private final AccessLogService accessLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (shouldRecord(request, response)) {
                accessLogService.record(
                        ClientIpResolver.resolve(request),
                        request.getRequestURI(),
                        request.getMethod(),
                        response.getStatus()
                );
            }
        }
    }

    private boolean shouldRecord(HttpServletRequest request, HttpServletResponse response) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();

        if (!RECORDABLE_METHODS.contains(method)) {
            return false;
        }
        if (status >= 500) {
            return true;
        }
        if (path.startsWith("/api/admin/") && (status == 401 || status == 403)) {
            return true;
        }
        if (EXCLUDED_PATHS.contains(path)) {
            return false;
        }
        if (path.startsWith("/api/admin/") && MUTATION_METHODS.contains(method)) {
            return true;
        }
        return "POST".equals(method) && path.matches("/api/posts/[^/]+/comments");
    }
}
