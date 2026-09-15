package com.taskflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Demo 3 - Apply Custom Authentication Configuration.
 * Extended in Module 5 - Apply Form-Based Authentication with Remember-Me.
 * Extended in Module 7 - Apply Role-Based Authorization.
 * Replaced in Module 9 - Apply OAuth2 Resource Server with JWT.
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
                        .anyRequest().permitAll()
                )
                // Stateless on purpose: every request carries its own JWT,
                // there is nothing for a server-side session to hold.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
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
     * that role came from our own DemoUserDetailsService or, as of this
     * module, from a Keycloak-issued JWT.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("MANAGER")
                .role("MANAGER").implies("USER")
                .build();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}



