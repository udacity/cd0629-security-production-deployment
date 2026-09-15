package com.udabank.authdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verify the four requirements from the exercise brief:
 *   (a) /public/** is open with no credentials
 *   (b) /app/** requires authentication
 *   (c) a valid DB-backed user (Argon2id password) can authenticate
 *   (d) HTTP Basic works and form login is disabled
 *
 * TODO: fill in each test body. Seed users are in data.sql:
 *   alice / VaultAdmin!2025  (ADMIN)
 *   bob   / QaTester!2025    (USER)
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointIsAccessibleWithoutAuthentication() throws Exception {
        // TODO: GET /public/health should return 200 with no credentials
    }

    @Test
    void appEndpointRejectsAnonymousRequests() throws Exception {
        // TODO: GET /app/dashboard with no credentials should return 401
    }

    @Test
    void appEndpointAcceptsValidDbBackedCredentials() throws Exception {
        // TODO: GET /app/dashboard with httpBasic("alice", "VaultAdmin!2025") should return 200
    }

    @Test
    void appEndpointRejectsWrongPassword() throws Exception {
        // TODO: GET /app/dashboard with httpBasic("alice", "wrong-password") should return 401
    }
}
