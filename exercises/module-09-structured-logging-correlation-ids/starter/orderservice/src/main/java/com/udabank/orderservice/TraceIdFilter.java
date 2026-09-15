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

/**
 * TODO: implement doFilter below — inventoryservice's TraceIdFilter (in
 * the other module in this exercise) is the exact same pattern, already
 * working. Mirror it here:
 *
 *   1. Read the "traceparent" request header.
 *   2. If it's present and well-formed (W3C format:
 *      "00-{32 hex trace-id}-{16 hex parent span-id}-{2 hex flags}"),
 *      extract the trace-id (the second dash-separated segment).
 *   3. If it's absent (or malformed), generate a new 32-hex-char trace-id
 *      instead — this request is starting a new trace.
 *   4. Either way, generate a fresh 16-hex-char span-id for this hop.
 *   5. Put "trace.id", "span.id", and "correlation.id" (same value as
 *      trace-id) into MDC before continuing the filter chain, and remove
 *      them in a finally block afterward.
 *
 * generateHex(...) below is given — use it for both the trace-id (16
 * bytes -> 32 hex chars) and span-id (8 bytes -> 16 hex chars).
 */
@Component
public class TraceIdFilter extends HttpFilter {

    private static final String TRACEPARENT_HEADER = "traceparent";
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // TODO: implement per the class-level comment above
        chain.doFilter(request, response);
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
