package com.mysociety.operations;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

final class CorrelationIds {
    static final String HEADER = "X-Correlation-Id";

    private CorrelationIds() {
    }

    static String current() {
        return MDC.get("correlationId");
    }
}

@Component
class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String value = request.getHeader(CorrelationIds.HEADER);
        String correlationId = value != null && value.matches("[A-Za-z0-9-]{1,100}") ? value : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        response.setHeader(CorrelationIds.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
