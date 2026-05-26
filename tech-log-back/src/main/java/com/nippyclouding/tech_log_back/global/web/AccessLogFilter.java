package com.nippyclouding.tech_log_back.global.web;

import com.nippyclouding.tech_log_back.log.access.service.AccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final Set<String> RECORDABLE_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> MUTATION_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> EXCLUDED_PATHS = Set.of("/api/admin/access-logs", "/api/admin/login-logs");

    private final AccessLogService accessLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean requestFailed = false;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException ex) {
            requestFailed = true;
            throw ex;
        } finally {
            int status = requestFailed && response.getStatus() < 500 ? 500 : response.getStatus();
            if (shouldRecord(request, status)) {
                recordSafely(request, status);
            }
        }
    }

    private void recordSafely(HttpServletRequest request, int status) {
        try {
            accessLogService.record(
                    ClientIpResolver.resolve(request),
                    request.getRequestURI(),
                    request.getMethod(),
                    status
            );
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to persist access audit log requestId={} method={} uri={} status={}",
                    RequestIdFilter.getRequestId(request),
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    ex
            );
        }
    }

    private boolean shouldRecord(HttpServletRequest request, int status) {
        String method = request.getMethod();
        String path = request.getRequestURI();

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
