package com.taskflow.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stands in for a real user repository. Three hardcoded users, one per
 * role, so Module 7 has something real to test the role hierarchy against.
 *
 * The password below is an Argon2id hash produced by
 * com.taskflow.util.PasswordHashGenerator (see that class + the README
 * for how to regenerate it for a password of your choice). All three
 * users share the same password on purpose, to keep the demo focused on
 * roles, not on managing three different credentials.
 *
 * DEMO STEP (Module 3): flip a user's `active` flag to false to show
 * CustomAuthenticationProvider rejecting a deactivated account.
 */
// RETIRED as of Module 9 — Keycloak now owns this. Left inert for reference.
// @Component
public class DemoUserDetailsService implements UserDetailsService {

    // Argon2id hash of "password123", produced by
    // Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().
    // Run com.taskflow.util.PasswordHashGenerator to regenerate this for
    // a different plaintext password, then paste the output here.
    private static final String PASSWORD_HASH =
            "REPLACE_ME_RUN_PasswordHashGenerator_AND_PASTE_OUTPUT_HERE";

    private static final Map<String, AppUserDetails> USERS = Map.of(
            "dev1", new AppUserDetails("dev1", PASSWORD_HASH, true, "USER"),
            "manager1", new AppUserDetails("manager1", PASSWORD_HASH, true, "MANAGER"),
            "admin1", new AppUserDetails("admin1", PASSWORD_HASH, true, "ADMIN")
    );

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserDetails user = USERS.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("No user found for: " + username);
        }
        return user;
    }
}

