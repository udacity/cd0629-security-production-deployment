package com.taskflow.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory failed-login tracking, deliberately simple for the demo (a
 * real app would use a database or a shared cache like Redis, so lockouts
 * survive a restart and work across multiple instances).
 *
 * The threshold is externalized via security.lockout.max-attempts in
 * application.yml rather than hardcoded, so it can change per environment
 * without a code change.
 */
// RETIRED as of Module 9 — Keycloak now owns this. Left inert for reference.
// @Component
public class LoginAttemptService {

    @Value("${security.lockout.max-attempts:5}")
    private int maxAttempts;

    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        failedAttempts.computeIfAbsent(username, u -> new AtomicInteger(0)).incrementAndGet();
    }

    public void loginSucceeded(String username) {
        failedAttempts.remove(username);
    }

    public boolean isBlocked(String username) {
        AtomicInteger count = failedAttempts.get(username);
        return count != null && count.get() >= maxAttempts;
    }
}
