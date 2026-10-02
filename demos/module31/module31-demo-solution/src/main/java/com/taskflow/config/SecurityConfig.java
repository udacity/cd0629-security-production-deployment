package com.taskflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.CrossOriginEmbedderPolicyHeaderWriter;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Demo 3 - Apply Custom Authentication Configuration.
 * Extended in Module 5 - Apply Form-Based Authentication with Remember-Me.
 * Extended in Module 7 - Apply Role-Based Authorization.
 * Replaced in Module 9 - Apply OAuth2 Resource Server with JWT.
 * Extended in Module 11 - Apply CORS and Security Header Configuration.
 *
 * As of Module 9, this no longer manages passwords, sessions, or
 * lockout itself — Keycloak does. What we built by hand in Modules 3
 * and 5 (com.taskflow.security.CustomAuthenticationProvider,
 * DemoUserDetailsService, CustomAuthenticationFailureHandler,
 * LoginAttemptService) is retired, not deleted — those classes are
 * still in the project, @Component commented out, kept as a reference
 * for what a real Authorization Server now does for us.
 *
 * The role hierarchy and method security from Module 7 are unchanged:
 * they don't care where the roles came from, only that they exist on
 * the Authentication object.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/tasks").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tasks/**").hasRole("MANAGER")
                        // Module 23: /actuator/refresh triggers a live config
                        // reload — genuinely sensitive, and was implicitly
                        // wide open this whole time under anyRequest().
                        // permitAll() below. /actuator/health and
                        // /actuator/prometheus stay open deliberately: in a
                        // real deployment these are typically reachable only
                        // from inside the private network (probes, scrapers),
                        // not gated by application-level auth at all — that's
                        // a network-isolation trade-off, not an oversight.
                        .requestMatchers("/actuator/refresh").hasRole("MANAGER")
                        .anyRequest().permitAll()
                )
                // Stateless on purpose: every request carries its own JWT,
                // there is nothing for a server-side session to hold.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // Explicit allowlist, not a wildcard. See
                // corsConfigurationSource() below for what's actually allowed.
                .cors(Customizer.withDefaults())
                // No cookies, no session, nothing ambient for a forged
                // request to ride along on — CSRF isn't a meaningful risk
                // for a stateless JWT API. 
                .csrf(csrf -> csrf.disable())
                // Worth knowing before testing: frameOptions
                // and contentTypeOptions below produce NO visible change —
                // Spring Security ships X-Frame-Options: DENY and
                // X-Content-Type-Options: nosniff by default, with zero
                // configuration. They're written explicitly here anyway,
                // so this protection survives even if someone later calls
                // .headers(headers -> headers.defaultsDisabled()...).
                // httpStrictTransportSecurity also won't appear in local
                // testing at all — that header only sends over an actual
                // secure (HTTPS) connection. The genuinely new headers,
                // absent before this block and present after, are:
                // Content-Security-Policy, Referrer-Policy, and the three
                // cross-origin isolation headers plus Permissions-Policy.
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("frame-ancestors 'none'; script-src 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .crossOriginOpenerPolicy(coop -> coop
                                .policy(CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN))
                        .crossOriginEmbedderPolicy(coep -> coep
                                .policy(CrossOriginEmbedderPolicyHeaderWriter.CrossOriginEmbedderPolicy.REQUIRE_CORP))
                        .crossOriginResourcePolicy(corp -> corp
                                .policy(CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=()"))
                );

        return http.build();
    }

    /**
     * Module 11: explicit allowlist for the one frontend origin we trust,
     * localhost:3000. Never a wildcard — a wildcard origin can't be paired
     * with credentialed requests anyway, and it defeats the entire point
     * of CORS as a trust boundary.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Keycloak puts realm roles in a claim shaped like:
     * "realm_access": { "roles": ["USER", "MANAGER"] }
     * — not the flat "scope" claim Spring's default converter expects.
     * Without this, the JWT would validate fine but every role check
     * would fail, since Spring would find zero authorities on the token.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        // Without this, authentication.getName() returns Keycloak's
        // internal subject UUID instead of the username — which would
        // silently break every ownership check built in Module 7
        // (#task.owner == authentication.name).
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return List.of();
        }
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * Unchanged since Module 7. ADMIN inherits everything MANAGER can do,
     * MANAGER inherits everything USER can do — regardless of whether
     * that role came from our own DemoUserDetailsService or, as of Module
     * 9, from a Keycloak-issued JWT.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("MANAGER")
                .role("MANAGER").implies("USER")
                .build();
    }

    @Bean
    @SuppressWarnings("deprecation") // no full replacement for method security role hierarchy yet — see spring-security#12783
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}
