package com.nippyclouding.tech_log_back.global.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

import com.nippyclouding.tech_log_back.log.access.service.AccessLogService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AccessLogFilterTest {

    @Mock
    private AccessLogService accessLogService;

    @Test
    void recordsAdminMutation() throws Exception {
        filter("PUT", "/api/admin/posts/1", 200);

        verify(accessLogService).record("127.0.0.1", "/api/admin/posts/1", "PUT", 200, "unassigned", null, null, null);
    }

    @Test
    void recordsCommentCreation() throws Exception {
        filter("POST", "/api/posts/1/comments", 201);

        verify(accessLogService).record("127.0.0.1", "/api/posts/1/comments", "POST", 201, "unassigned", null, null, null);
    }

    @Test
    void recordsAdminAuthorizationFailure() throws Exception {
        filter("GET", "/api/admin/comments", 403);

        verify(accessLogService).record("127.0.0.1", "/api/admin/comments", "GET", 403, "unassigned", null, null, null);
    }

    @Test
    void recordsServerFailureForPublicRequest() throws Exception {
        filter("GET", "/api/posts", 500);

        verify(accessLogService).record("127.0.0.1", "/api/posts", "GET", 500, "unassigned", null, null, null);
    }

    @Test
    void skipsNormalPublicRequestAndAuthenticationCheck() throws Exception {
        filter("GET", "/api/posts", 200);
        filter("GET", "/api/auth/me", 200);

        verify(accessLogService, never()).record("127.0.0.1", "/api/posts", "GET", 200, "unassigned", null, null, null);
        verify(accessLogService, never()).record("127.0.0.1", "/api/auth/me", "GET", 200, "unassigned", null, null, null);
    }

    @Test
    void skipsNormalLogLookupAndBrowserAuxiliaryRequests() throws Exception {
        filter("GET", "/api/admin/access-logs", 200);
        filter("OPTIONS", "/api/admin/posts", 500);
        filter("HEAD", "/api/posts", 500);
        filter("TRACE", "/api/posts", 500);

        verify(accessLogService, never()).record("127.0.0.1", "/api/admin/access-logs", "GET", 200, "unassigned", null, null, null);
        verify(accessLogService, never()).record("127.0.0.1", "/api/admin/posts", "OPTIONS", 500, "unassigned", null, null, null);
        verify(accessLogService, never()).record("127.0.0.1", "/api/posts", "HEAD", 500, "unassigned", null, null, null);
        verify(accessLogService, never()).record("127.0.0.1", "/api/posts", "TRACE", 500, "unassigned", null, null, null);
    }

    @Test
    void recordsFailureOnLogLookup() throws Exception {
        filter("GET", "/api/admin/access-logs", 500);

        verify(accessLogService).record("127.0.0.1", "/api/admin/access-logs", "GET", 500, "unassigned", null, null, null);
    }

    @Test
    void recordsEscapingRequestExceptionAsServerFailure() {
        AccessLogFilter filter = new AccessLogFilter(accessLogService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new RuntimeException("request failed");
        }));

        verify(accessLogService).record(
                eq("127.0.0.1"),
                eq("/api/posts"),
                eq("GET"),
                eq(500),
                eq("unassigned"),
                eq(RuntimeException.class.getName()),
                eq("request failed"),
                contains("RuntimeException: request failed")
        );
    }

    @Test
    void recordsHandledServerFailureDetails() throws Exception {
        AccessLogFilter filter = new AccessLogFilter(accessLogService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            RequestFailureDetails.capture(request, new IllegalStateException("handled failure"));
            response.setStatus(500);
        });

        verify(accessLogService).record(
                eq("127.0.0.1"),
                eq("/api/posts"),
                eq("GET"),
                eq(500),
                eq("unassigned"),
                eq(IllegalStateException.class.getName()),
                eq("handled failure"),
                contains("IllegalStateException: handled failure")
        );
    }

    @Test
    void doesNotFailCompletedRequestWhenAuditPersistenceFails() {
        doThrow(new RuntimeException("database unavailable"))
                .when(accessLogService).record("127.0.0.1", "/api/admin/posts/1", "PUT", 200, "unassigned", null, null, null);

        assertDoesNotThrow(() -> filter("PUT", "/api/admin/posts/1", 200));
    }

    private void filter(String method, String path, int status) throws Exception {
        AccessLogFilter filter = new AccessLogFilter(accessLogService);
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) -> response.setStatus(status);

        filter.doFilter(request, response, filterChain);
    }
}
