package com.taskflow.config;

import com.taskflow.security.CustomAuthenticationFailureHandler;
import com.taskflow.security.CustomAuthenticationProvider;
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
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Demo 3 - Apply Custom Authentication Configuration.
 * Extended in Module 5 - Apply Form-Based Authentication with Remember-Me.
 * Extended in Module 7 - Apply Role-Based Authorization.
 *
 * Everything here is lambda DSL only, per Spring Security 7's requirement
 * (the old chained .and() builder style is gone).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            CustomAuthenticationProvider customAuthenticationProvider,
                                            CustomAuthenticationFailureHandler customAuthenticationFailureHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/tasks").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tasks/**").hasRole("MANAGER")
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

    /**
     * Module 7: ADMIN inherits everything MANAGER can do, MANAGER inherits
     * everything USER can do. So an ADMIN never needs ROLE_MANAGER granted
     * directly to pass a hasRole("MANAGER") check.
     * Role hierarychy: admin, manager, user
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("MANAGER")
                .role("MANAGER").implies("USER")
                .build();
    }

    /**
     * Without this bean, the role hierarchy above only applies to URL-level
     * hasRole() checks. @PreAuthorize checks (method-level) would silently
     * ignore it unless this handler wires the hierarchy in explicitly.
     * Note: setRoleHierarchy deprecated, no full replacement for method security role hierarchy yet — see spring-security#12783
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}
