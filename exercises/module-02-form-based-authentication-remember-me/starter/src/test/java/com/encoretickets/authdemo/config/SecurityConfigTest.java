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

/**
 * Seed users (see data.sql):
 *   alice / BoxOffice!2025      (ADMIN)
 *   bob   / FrontRow!2025       (USER)  — used for the redirect + remember-me checks
 *   carol / BackstagePass!2025  (USER)  — dedicated to the lockout check so it
 *                                          doesn't interfere with bob's other tests
 *
 * TODO: fill in each test body below.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void loginPageIsAccessible() throws Exception {
        // TODO: GET /login should return 200
    }

    @Test
    void adminIsRedirectedToAdminConsoleOnSuccessfulLogin() throws Exception {
        // TODO: POST /login with .with(csrf()) and alice's credentials should
        // redirect to "/admin"
    }

    @Test
    void regularUserIsRedirectedToDashboardOnSuccessfulLogin() throws Exception {
        // TODO: POST /login with .with(csrf()) and bob's credentials should
        // redirect to "/dashboard"
    }

    @Test
    void rememberMeCookieIsSetWhenRequested() throws Exception {
        // TODO: POST /login with .with(csrf()), bob's credentials, and
        // param("remember-me", "true") should set a cookie named "remember-me"
    }

    @Test
    void accountLocksAfterFiveFailedAttempts() throws Exception {
        // TODO:
        //   1. POST /login with carol's username and a wrong password, 5 times.
        //      Each attempt should redirect to "/login?error".
        //   2. POST /login again with carol's CORRECT password — it should still
        //      redirect to "/login?error" because the account is now locked.
        //   3. Assert userRepository.findByUsername("carol") has accountLocked == true.
    }
}
