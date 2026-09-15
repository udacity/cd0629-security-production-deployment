package com.harborlight.authdemo.config;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

public class CspNonceHeaderWriter implements HeaderWriter {

    public static final String NONCE_REQUEST_ATTRIBUTE = "cspNonce";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String nonce = generateNonce();
        request.setAttribute(NONCE_REQUEST_ATTRIBUTE, nonce);
        response.setHeader("Content-Security-Policy", buildPolicy(nonce));
    }

    private String buildPolicy(String nonce) {
        return String.join("; ",
                "default-src 'self'",
                "script-src 'self' 'nonce-" + nonce + "'",
                "frame-ancestors 'none'",
                "report-uri /csp-reports");
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }
}
