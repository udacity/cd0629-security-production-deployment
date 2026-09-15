package com.udabank.orderservice;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TraceIdFilter extends HttpFilter {

    private static final String TRACEPARENT_HEADER = "traceparent";
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        String traceId = extractTraceId(traceparent).orElseGet(() -> generateHex(16));
        String spanId = generateHex(8);

        MDC.put("trace.id", traceId);
        MDC.put("span.id", spanId);
        MDC.put("correlation.id", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("trace.id");
            MDC.remove("span.id");
            MDC.remove("correlation.id");
        }
    }

    private Optional<String> extractTraceId(String traceparent) {
        if (traceparent == null) {
            return Optional.empty();
        }
        String[] parts = traceparent.split("-");
        if (parts.length != 4 || parts[1].length() != 32) {
            return Optional.empty();
        }
        return Optional.of(parts[1]);
    }

    private String generateHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(byteLength * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
