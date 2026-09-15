package com.taskflow.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Records every failed login attempt against LoginAttemptService, then
 * falls back to Spring's normal "redirect to /login?error" behavior.
 *
 * This handler only counts failures. The actual blocking decision lives
 * in CustomAuthenticationProvider, right next to the other rule we added
 * ourselves back in Module 3 (the active-account check) — same pattern,
 * one more custom rule.
 */
// RETIRED as of Module 9 — Keycloak now owns this. Left inert for reference.
// @Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public CustomAuthenticationFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username != null) {
            loginAttemptService.loginFailed(username);
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
