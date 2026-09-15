package com.harborlight.authdemo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Harborlight — CORS and Security Header Configuration
 *
 * Theo's incident: a lookalike domain iframed Harborlight's real donation
 * page to phish donors. The headers below (all but CSP) and the SPA-cookie
 * CSRF setup are already wired as worked examples — matching the demos
 * verbatim. Two things are missing:
 *
 *   1. corsConfigurationSource() below — right now it's wide open ("*"),
 *      so literally any site's JavaScript can call this API. Lock it to
 *      Harborlight's real SPA origin only.
 *   2. CspNonceHeaderWriter.buildPolicy() (separate file) — the header
 *      writer plumbing is done, but the actual policy string is a TODO
 *      there.
 */
@Configuration
public class SecurityConfig {

    private static final String SPA_ORIGIN = "https://app.harborlight.example";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                        // Browsers deliver CSP violation reports automatically — they won't
                        // have (or know about) our CSRF cookie, so this path has to be exempt.
                        .ignoringRequestMatchers("/csp-reports"))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31_536_000)) // 1 year
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("geolocation=(), camera=(), microphone=()"))
                        .addHeaderWriter(new CspNonceHeaderWriter()));

        return http.build();
    }

    // TODO: restrict this to Harborlight's real SPA origin (SPA_ORIGIN above),
    // the specific HTTP methods the frontend actually calls, and allow
    // credentials (the CSRF cookie has to travel with cross-origin requests).
    // Right now it permits any origin, which is exactly the hole Theo's
    // incident exposed — any site's JS can call this API as if it were
    // the real frontend.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
