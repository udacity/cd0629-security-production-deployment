package com.encoretickets.authdemo.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.encoretickets.authdemo.security.AccountLockoutAuthenticationFailureHandler;
import com.encoretickets.authdemo.security.RoleBasedAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    private final RoleBasedAuthenticationSuccessHandler successHandler;
    private final AccountLockoutAuthenticationFailureHandler failureHandler;

    public SecurityConfig(RoleBasedAuthenticationSuccessHandler successHandler,
                           AccountLockoutAuthenticationFailureHandler failureHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        tokenRepository.setCreateTableOnStartup(true);
        return tokenRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PersistentTokenRepository tokenRepository) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/login")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/admin/**")).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .rememberMe(remember -> remember
                        .tokenRepository(tokenRepository)
                        .key("encore-remember-me-key")
                        .tokenValiditySeconds(1209600)); // 14 days

        return http.build();
    }
}
