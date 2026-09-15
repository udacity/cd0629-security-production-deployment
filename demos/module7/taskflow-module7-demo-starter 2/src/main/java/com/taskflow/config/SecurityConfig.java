package com.taskflow.config;

import com.taskflow.security.CustomAuthenticationFailureHandler;
import com.taskflow.security.CustomAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            CustomAuthenticationProvider customAuthenticationProvider,
                                            CustomAuthenticationFailureHandler customAuthenticationFailureHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/tasks").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .failureHandler(customAuthenticationFailureHandler)
                        .defaultSuccessUrl("/api/v1/tasks", true)
                )
                .rememberMe(remember -> remember.key("taskflow-remember-key"))
                .sessionManagement(session -> session.maximumSessions(1))
                .authenticationProvider(customAuthenticationProvider)
                // Still off deliberately, same as Module 3 — CSRF gets its own
                // full module later. Worth saying out loud on camera: leaving
                // this off is fine for the demo, but a real session-based
                // login without CSRF protection is not production-safe.
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Spring Security 7's recommended password encoder. Argon2id, tuned
     * to the current Spring Security defaults (salt 16 bytes, hash 32
     * bytes, parallelism 1, memory 1<<14, 2 iterations).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}

