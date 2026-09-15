package com.udabank.authdemo.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * Solution walkthrough: "Inspecting the resolved filter chain".
 *
 * Prints the ordered list of Filter classes that make up the app's
 * SecurityFilterChain at startup, so you can see exactly what
 * httpBasic() + formLogin(disable) + authorizeHttpRequests() resolved to —
 * useful for confirming things like "is CsrfFilter present?" or
 * "where does BasicAuthenticationFilter sit relative to the
 * authorization filter?" without attaching a debugger.
 */
@Component
public class FilterChainInspector implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FilterChainInspector.class);

    private final List<SecurityFilterChain> filterChains;

    public FilterChainInspector(List<SecurityFilterChain> filterChains) {
        this.filterChains = filterChains;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SecurityFilterChain chain : filterChains) {
            log.info("Resolved SecurityFilterChain: {}", chain.getClass().getSimpleName());
            if (chain instanceof DefaultSecurityFilterChain defaultChain) {
                defaultChain.getFilters().forEach(filter ->
                        log.info("  -> {}", filter.getClass().getSimpleName()));
            }
        }
    }
}
