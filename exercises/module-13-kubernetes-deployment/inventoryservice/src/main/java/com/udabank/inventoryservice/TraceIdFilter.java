package com.udabank.inventoryservice;

import java.io.IOException;
import java.security.SecureRandom;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Given — a worked example for the same filter you'll write in
 * orderservice. Reads the W3C traceparent header
 * (https://www.w3.org/TR/trace-context/#traceparent-header) if the caller
 * sent one — format: "00-{32 hex trace-id}-{16 hex parent span-id}-{2 hex
 * flags}" — and continues that trace. If there's no traceparent (this is
 * the first hop of a request), generates a new trace-id instead. Either
 * way, this hop always gets its own fresh span-id.
 *
 * MDC "correlation.id" is set equal to the trace-id here — for this
 * exercise they're the same concept: one value that ties every log line
 * from every service back to one originating request.
 */
@Component
public class TraceIdFilter extends HttpFilter {

    private static final String TRACEPARENT_HEADER = "traceparent";
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        String traceId = extractTraceId(traceparent).orElseGet(this::generateHex32);
        String spanId = generateHex16();

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

    private java.util.Optional<String> extractTraceId(String traceparent) {
        if (traceparent == null) {
            return java.util.Optional.empty();
        }
        String[] parts = traceparent.split("-");
        if (parts.length != 4 || parts[1].length() != 32) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(parts[1]);
    }

    private String generateHex32() {
        return generateHex(16);
    }

    private String generateHex16() {
        return generateHex(8);
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
