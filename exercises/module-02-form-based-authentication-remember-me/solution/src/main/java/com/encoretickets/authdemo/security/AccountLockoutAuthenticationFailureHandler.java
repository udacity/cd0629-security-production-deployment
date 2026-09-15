package com.encoretickets.authdemo.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.encoretickets.authdemo.user.UserRepository;

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
        String username = request.getParameter("username");

        userRepository.findByUsername(username).ifPresent(user -> {
            user.incrementFailedAttempts();
            if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
                user.lock();
            }
            userRepository.save(user);
        });

        response.sendRedirect("/login?error");
    }
}
