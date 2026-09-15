package com.udabank.authdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/public/health"))
                .andExpect(status().isOk());
    }

    @Test
    void appEndpointRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appEndpointAcceptsValidDbBackedCredentials() throws Exception {
        mockMvc.perform(get("/app/dashboard").with(httpBasic("alice", "VaultAdmin!2025")))
                .andExpect(status().isOk());
    }

    @Test
    void appEndpointRejectsWrongPassword() throws Exception {
        mockMvc.perform(get("/app/dashboard").with(httpBasic("alice", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }
}
