package com.taskflow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * RETIRED as of Module 9 (Apply OAuth2 Resource Server with JWT).
 *
 * @Component is commented out below on purpose — this class no longer
 * participates in authentication at all. Keycloak now validates
 * credentials, and Spring's built-in JWT decoder validates the token's
 * signature. Left in the project, inert, as a reference for what a real
 * Authorization Server now does on our behalf: credential checking,
 * account-status checking, and failed-login tracking, all of it.
 *
 * Demo 3's centerpiece, extended in Module 5: a fully custom
 * AuthenticationProvider.
 *
 * It does the normal work a provider does (look up the user, check the
 * password) and adds rules that are entirely our own: the account must
 * be active (Module 3), and it must not be locked out from too many
 * failed attempts (Module 5). Neither check is part of Spring's built-in
 * UserDetails contract, on purpose, so it's obvious this logic belongs
 * to us, not to the framework.
 */
// @Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService,
                                         PasswordEncoder passwordEncoder,
                                         LoginAttemptService loginAttemptService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = String.valueOf(authentication.getCredentials());

        log.info("Custom provider checked {}", username);

        if (loginAttemptService.isBlocked(username)) {
            log.warn("Blocked login attempt for {} — too many failed attempts", username);
            throw new LockedException("Account temporarily locked due to too many failed attempts");
        }

        UserDetails user = userDetailsService.loadUserByUsername(username);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (user instanceof AppUserDetails appUser && !appUser.isActive()) {
            log.warn("Rejected {} — account is deactivated", username);
            throw new DisabledException("Account is deactivated");
        }

        loginAttemptService.loginSucceeded(username);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
