package com.harborlight.authdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests 1-2 are given as worked examples (they check headers that are
 * already fully configured). Tests 3-4 check your two TODOs — fill them in.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hstsHeaderIsPresentWithExpectedDirectives() throws Exception {
        // Spring Security only emits HSTS over HTTPS (it's a no-op on plain HTTP
        // by design — telling a browser "always use HTTPS" over an insecure
        // channel doesn't mean anything). .secure(true) simulates an HTTPS
        // request so this test can verify the config without real TLS.
        mockMvc.perform(get("/api/campaigns").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
                .andExpect(header().string("Strict-Transport-Security", containsString("includeSubDomains")))
                .andExpect(header().string("Strict-Transport-Security", containsString("preload")));
    }

    @Test
    void frameOptionsHeaderDeniesFraming() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void corsPreflightRejectsUntrustedOrigin() throws Exception {
        mockMvc.perform(options("/api/campaigns")
                        .header("Origin", "https://evil-lookalike.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void corsPreflightAcceptsHarborlightSpaOrigin() throws Exception {
        mockMvc.perform(options("/api/campaigns")
                        .header("Origin", "https://app.harborlight.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.harborlight.example"));
    }
}
