package com.udabank.authdemo.invoice;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.io.ClassPathResource;

/**
 * Given — not part of the exercise. Stands in for Udabank's real auth
 * service: mints RS256-signed JWTs using the test-only private key in
 * src/test/resources/keys/private_key.pem (its matching public key is
 * what SecurityConfig's JwtDecoder validates against).
 */
public final class TestJwtIssuer {

    private TestJwtIssuer() {
    }

    public static String issueToken(String subject, String... entitlements) throws Exception {
        RSAPrivateKey privateKey = loadPrivateKey();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("https://auth.udabank.example")
                .claim("entitlements", List.of(entitlements))
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signedJWT.sign(new RSASSASigner(privateKey));
        return signedJWT.serialize();
    }

    private static RSAPrivateKey loadPrivateKey() throws Exception {
        String pem = new ClassPathResource("keys/private_key.pem").getContentAsString(StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }
}
