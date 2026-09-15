package com.udabank.authdemo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Udabank Partner API — OAuth2 Resource Server with JWT
 *
 * The filter chain and the JwtDecoder (validating tokens against Udabank
 * auth's public key) are already wired below — that's the "Configuring the
 * app as an OAuth2 resource server" demo, verbatim. What's missing is
 * jwtAuthenticationConverter(): the piece that turns a decoded token's
 * claims into Spring Security authorities your @PreAuthorize checks can
 * actually test.
 *
 * One wrinkle: Udabank's partner tokens were designed before anyone
 * standardized on the OAuth2 "scope" claim, so grants show up under a
 * claim called "entitlements" instead — a JSON array like ["read","write"].
 *
 * TODO: implement jwtAuthenticationConverter() below. Building blocks:
 *   - JwtGrantedAuthoritiesConverter has setAuthoritiesClaimName(String)
 *     to point it at a claim other than the "scope"/"scp" default, and
 *     setAuthorityPrefix(String) to control the authority prefix (you want
 *     it to match what @PreAuthorize("hasAuthority('SCOPE_...')")  expects).
 *   - JwtAuthenticationConverter has setJwtGrantedAuthoritiesConverter(...)
 *     to plug in a converter you built.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                // Stateless API: partners present a bearer token on every request,
                // there's no browser session/cookie for CSRF to protect.
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return NimbusJwtDecoder.withPublicKey(loadPublicKey()).build();
    }

    // TODO: implement this bean (see the class-level comment for the building blocks)
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    private RSAPublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = new ClassPathResource("keys/public_key.pem").getContentAsString(StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }
}
