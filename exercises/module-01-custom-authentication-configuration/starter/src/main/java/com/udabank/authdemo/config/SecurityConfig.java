package com.udabank.authdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Udabank — Custom Authentication Configuration
 *
 * The CTO wants a security baseline shipped before Friday's compliance review:
 *
 *   (a) /public/**  -> open to everyone, no login
 *   (b) /app/**     -> requires an authenticated user, loaded from the `users` table
 *                      via DbUserDetailsService
 *   (c) passwords   -> hashed with Argon2id (OWASP-recommended default)
 *   (d) form login  -> disabled for now; HTTP Basic enabled instead so QA can hit
 *                      the API directly from curl/Postman while the frontend team
 *                      builds the real login page
 *
 * The /public/** rule, httpBasic, and formLogin(disable) are already wired below
 * as a worked example — mirror that pattern for the two TODOs.
 *
 * TODO 1: Define a PasswordEncoder bean using Argon2PasswordEncoder
 *         (org.springframework.security.crypto.argon2.Argon2PasswordEncoder).
 *         Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8() gives you
 *         OWASP-recommended parameters (m=19456, t=2, p=1) in one call.
 *
 * TODO 2: Add two more authorizeHttpRequests rules below the /public/** example:
 *           - "/app/**"   -> authenticated()
 *           - anyRequest() -> denyAll()   (fail closed instead of fail open)
 *
 * DbUserDetailsService is already a @Service bean, so once it's implemented
 * Spring Security will pick it up automatically — no extra wiring needed here.
 */
@Configuration
public class SecurityConfig {

    // TODO 1: PasswordEncoder bean (Argon2id)
    // @Bean
    // public PasswordEncoder passwordEncoder() {
    //     return ...
    // }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/public/**")).permitAll()
                        // TODO 2: add "/app/**" -> authenticated() and anyRequest() -> denyAll() here
                        )
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
