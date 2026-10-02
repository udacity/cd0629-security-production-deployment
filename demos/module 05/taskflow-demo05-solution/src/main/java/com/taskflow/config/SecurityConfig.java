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

/**
 * Demo 3 - Apply Custom Authentication Configuration.
 * Extended in Module 5 - Apply Form-Based Authentication with Remember-Me.
 *
 * Everything here is lambda DSL only, per Spring Security 7's requirement
 * (the old chained .and() builder style is gone).
 */
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
                /*previously
                .httpBasic(Customizer.withDefaults())*/

                .formLogin(form -> form
                        .failureHandler(customAuthenticationFailureHandler)
                        .defaultSuccessUrl("/api/v1/tasks", true)
                )
                /* remember me on this computer*/ 
                .rememberMe(remember -> remember.key("taskflow-remember-key"))
                /* one active session per user*/ 
                .sessionManagement(session -> session.maximumSessions(1))
                .authenticationProvider(customAuthenticationProvider)
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

