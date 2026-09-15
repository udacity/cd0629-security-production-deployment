package com.harborlight.authdemo.config;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

/**
 * Given — mostly plumbing, not the exercise. Spring Security's headers()
 * DSL only supports a single static Content-Security-Policy string
 * (contentSecurityPolicy(csp -> csp.policyDirectives("..."))), but a CSP
 * nonce has to be random per request — so this is written by hand as a
 * HeaderWriter instead, generating a fresh nonce on every request and
 * exposing it as a request attribute (a real server-rendered page would
 * read that attribute to put the *same* nonce on its <script> tags).
 *
 * TODO: fill in buildPolicy(nonce). This is the actual security decision
 * this exercise is about — see the TODO comment on that method below.
 */
public class CspNonceHeaderWriter implements HeaderWriter {

    public static final String NONCE_REQUEST_ATTRIBUTE = "cspNonce";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String nonce = generateNonce();
        request.setAttribute(NONCE_REQUEST_ATTRIBUTE, nonce);
        response.setHeader("Content-Security-Policy", buildPolicy(nonce));
    }

    /**
     * TODO: compose the CSP policy string for this request's nonce. Theo's
     * incident was a lookalike domain iframing the real donation page to
     * phish donors — your policy needs to stop that AND stop injected
     * inline scripts, using only trusted-by-nonce scripts. Required
     * directives:
     *   - default-src 'self'                         (baseline: same-origin only)
     *   - script-src 'self' 'nonce-<the nonce>'       (only scripts you served, or ones tagged with this request's nonce — NOT 'unsafe-inline')
     *   - frame-ancestors 'none'                      (nobody, including the phishing clone, may iframe this page — stronger than X-Frame-Options)
     *   - report-uri /csp-reports                     (send violation reports here — see the solution walkthrough)
     * Join directives with "; ".
     */
    private String buildPolicy(String nonce) {
        // TODO: build and return the real policy string described above
        return "default-src 'self'";
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }
}
