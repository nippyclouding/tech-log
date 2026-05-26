package com.nippyclouding.tech_log_back.global.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;

public record RequestFailureDetails(String errorType, String errorMessage, String stackTrace) {

    private static final String ATTRIBUTE = RequestFailureDetails.class.getName() + ".failure";
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_STACK_TRACE_LENGTH = 8000;

    public static void capture(HttpServletRequest request, Throwable throwable) {
        if (request.getAttribute(ATTRIBUTE) != null) {
            return;
        }

        StringWriter buffer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(buffer));
        request.setAttribute(ATTRIBUTE, new RequestFailureDetails(
                throwable.getClass().getName(),
                truncate(throwable.getMessage(), MAX_MESSAGE_LENGTH),
                truncate(buffer.toString(), MAX_STACK_TRACE_LENGTH)
        ));
    }

    public static RequestFailureDetails from(HttpServletRequest request) {
        Object details = request.getAttribute(ATTRIBUTE);
        return details instanceof RequestFailureDetails failure ? failure : null;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
