package com.nippyclouding.tech_log_back.global.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    @Test
    void assignsRequestIdToResponseAndLoggingContext() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> loggingRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                loggingRequestId.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotBlank();
        assertThat(RequestIdFilter.getRequestId(request)).isEqualTo(response.getHeader(RequestIdFilter.HEADER));
        assertThat(loggingRequestId.get()).isEqualTo(response.getHeader(RequestIdFilter.HEADER));
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
