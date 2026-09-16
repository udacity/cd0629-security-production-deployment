package com.taskflow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Module 19: implements the MDC pattern Module 18 described — a
 * thread-local map that attaches metadata to every log line for the
 * life of one request, automatically, without touching individual log
 * statements.
 *
 * Simplified from the real W3C Trace Context standard Module 18 also
 * covered (the full "traceparent" header has a specific structured
 * format: version-traceid-spanid-flags). Here we use a plain custom
 * header, X-Trace-Id, for clarity — same mechanism (extract an
 * incoming ID, or generate one if this is the first hop; bind it to
 * MDC; every log line during this request carries it automatically),
 * just not the exact wire format a production system would use.
 *
 * @Order(HIGHEST_PRECEDENCE) matters here: it puts this filter outside
 * Spring Security's own filter chain, so even a request Security
 * rejects (401/403) still gets a trace ID in its response and in
 * whatever Security itself logs about the rejection.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        boolean generated = (traceId == null || traceId.isBlank());
        if (generated) {
            traceId = UUID.randomUUID().toString();
        }

        // Every log statement anywhere in this request's call stack now
        // automatically includes traceId, with no changes needed to any
        // individual log.info(...) call, and Spring Boot's structured
        // (JSON) logging picks up MDC values automatically.
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Critical: MDC is thread-local, and threads get reused
            // (thread pools, virtual thread carriers). Without this,
            // one request's trace ID can leak into the next unrelated
            // request that happens to run on the same thread.
            MDC.remove(MDC_KEY);
        }
    }
}

