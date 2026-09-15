package com.encoretickets.authdemo.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.encoretickets.authdemo.user.UserRepository;

/**
 * TODO: on every failed login attempt:
 *   1. Read the submitted username via request.getParameter("username").
 *   2. Look the user up with userRepository.findByUsername(...).
 *   3. If found, call user.incrementFailedAttempts(), and once
 *      user.getFailedAttempts() reaches MAX_ATTEMPTS, call user.lock().
 *   4. Save the change with userRepository.save(user).
 *
 * The redirect to "/login?error" below is already correct — don't change it.
 */
@Component
public class AccountLockoutAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;

    public AccountLockoutAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        // TODO: implement the lookup + increment + lock logic described above

        response.sendRedirect("/login?error");
    }
}
