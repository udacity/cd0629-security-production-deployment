package com.udabank.orderservice;

import java.io.IOException;
import java.security.SecureRandom;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class TraceparentPropagatingInterceptor implements ClientHttpRequestInterceptor {

    private final SecureRandom random = new SecureRandom();

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String traceId = MDC.get("trace.id");
        if (traceId != null) {
            String spanId = generateHex(8);
            request.getHeaders().add("traceparent", "00-" + traceId + "-" + spanId + "-01");
        }
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
