package com.encoretickets.authdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.encoretickets.authdemo.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void loginPageIsAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void adminIsRedirectedToAdminConsoleOnSuccessfulLogin() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "alice")
                        .param("password", "BoxOffice!2025"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void regularUserIsRedirectedToDashboardOnSuccessfulLogin() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "bob")
                        .param("password", "FrontRow2025"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void rememberMeCookieIsSetWhenRequested() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "bob")
                        .param("password", "FrontRow2025")
                        .param("remember-me", "true"))
                .andExpect(cookie().exists("remember-me"));
    }

    @Test
    void accountLocksAfterFiveFailedAttempts() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login").with(csrf())
                            .param("username", "carol")
                            .param("password", "wrong-password"))
                    .andExpect(redirectedUrl("/login?error"));
        }

        // 6th attempt with the CORRECT password should still fail: the account is locked.
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "carol")
                        .param("password", "BackstagePass!2025"))
                .andExpect(redirectedUrl("/login?error"));

        assertThat(userRepository.findByUsername("carol").orElseThrow().isAccountLocked()).isTrue();
    }
}
