package com.taskflow.config;

import com.taskflow.security.CustomAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Demo 3 - Apply Custom Authentication Configuration.
 *
 * Everything here is lambda DSL only, per Spring Security 7's requirement
 * (the old chained .and() builder style is gone).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            CustomAuthenticationProvider customAuthenticationProvider) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/tasks").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .authenticationProvider(customAuthenticationProvider)
                // Demo API only, no browser session needed for these calls.
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
