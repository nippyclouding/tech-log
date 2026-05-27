package com.nippyclouding.tech_log_back.global.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsNotFoundForMissingStaticResource() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php"
        );
        NoResourceFoundException exception = new NoResourceFoundException(
                HttpMethod.GET,
                "api/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php",
                "classpath:/static/"
        );

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFoundException(exception, request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("COMMON_404", response.getBody().code());
        assertEquals("/api/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php", response.getBody().path());
    }
}
