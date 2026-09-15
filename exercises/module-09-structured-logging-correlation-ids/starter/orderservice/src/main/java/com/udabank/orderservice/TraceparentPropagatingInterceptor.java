package com.udabank.orderservice;

import java.io.IOException;
import java.security.SecureRandom;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * TODO: implement intercept() below. Registered on the RestClient
 * orderservice uses to call inventoryservice (see InventoryClient) — this
 * is what makes inventoryservice's TraceIdFilter see the SAME trace-id
 * orderservice is already using for this request, continuing the trace
 * instead of starting a new one.
 *
 *   1. Read the current trace-id from MDC (key "trace.id" — TraceIdFilter
 *      already put it there before this interceptor runs, since the
 *      filter runs first for the inbound request that triggered this
 *      outbound call).
 *   2. Generate a NEW span-id for this outbound hop (generateHex(8) below
 *      does this) — don't reuse the current span-id; each hop gets its own.
 *   3. Build a traceparent header value: "00-{trace-id}-{new span-id}-01"
 *      and add it to request.getHeaders() before calling execution.execute(...).
 *
 * If MDC has no trace-id (shouldn't happen once TraceIdFilter is wired,
 * but defensively), just skip adding the header and proceed.
 */
public class TraceparentPropagatingInterceptor implements ClientHttpRequestInterceptor {

    private final SecureRandom random = new SecureRandom();

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        // TODO: implement per the class-level comment above
        return execution.execute(request, body);
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
