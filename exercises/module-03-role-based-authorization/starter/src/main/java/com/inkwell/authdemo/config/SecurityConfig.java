package com.inkwell.authdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * URL-level rules and method security bootstrap — already done for you
 * (same PathPatternRequestMatcher pattern from module 1). This module's
 * exercise is entirely in DocumentService: URL-level "/admin/** requires
 * ADMIN" only handles coarse routing, it can't express "this specific
 * document belongs to this specific caller" — that's what @PreAuthorize
 * with a SpEL owner check is for.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/admin/**")).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                // Stateless API: clients present credentials on every request via HTTP
                // Basic, there's no browser session/cookie for CSRF to protect. CSRF
                // protection is for cookie-authenticated browser clients, not this.
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
