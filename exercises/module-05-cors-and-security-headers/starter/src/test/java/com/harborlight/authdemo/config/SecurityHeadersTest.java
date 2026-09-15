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
        // TODO: an OPTIONS preflight (see the `options(...)` import above)
        // to "/api/campaigns" with header "Origin" set to
        // "https://evil-lookalike.example" and header
        // "Access-Control-Request-Method" set to "GET" should NOT come back
        // with an Access-Control-Allow-Origin header for that origin.
        // Hint: MockMvcResultMatchers.header().doesNotExist(...) or
        // header().string(...) asserting it's not the evil origin.
    }

    @Test
    void corsPreflightAcceptsHarborlightSpaOrigin() throws Exception {
        // TODO: same OPTIONS preflight as above, but with Origin set to
        // "https://app.harborlight.example" — this one SHOULD get back
        // Access-Control-Allow-Origin: https://app.harborlight.example
    }
}
