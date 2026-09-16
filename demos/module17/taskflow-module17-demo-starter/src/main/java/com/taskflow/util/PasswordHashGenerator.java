package com.taskflow.util;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Run this directly from your IDE (right-click > Run) before the demo.
 * It prints an Argon2id hash you paste into DemoUserDetailsService.PASSWORD_HASH.
 *
 * Optional arg: the plaintext password to hash. Defaults to "password123",
 * matching what's used in the demo script and README.
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        String rawPassword = args.length > 0 ? args[0] : "password123";

        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String hash = encoder.encode(rawPassword);

        System.out.println();
        System.out.println("Plaintext password: " + rawPassword);
        System.out.println("Argon2id hash:       " + hash);
        System.out.println();
        System.out.println("Paste the hash above into DemoUserDetailsService.PASSWORD_HASH");
        System.out.println();
    }
}
