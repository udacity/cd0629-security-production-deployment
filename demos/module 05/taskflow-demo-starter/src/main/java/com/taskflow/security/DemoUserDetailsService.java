package com.taskflow.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real user repository. One hardcoded user, dev1.
 *
 * The password below is an Argon2id hash produced by
 * com.taskflow.util.PasswordHashGenerator (see that class + the README
 * for how to regenerate it for a password of your choice).
 *
 * DEMO STEP: flip ACTIVE to false, restart the app, and re-run the same
 * curl request from earlier in the demo to show CustomAuthenticationProvider
 * rejecting a deactivated account for a reason that has nothing to do with
 * the password being wrong.
 */
@Component
public class DemoUserDetailsService implements UserDetailsService {

    // DEMO: flip this to false to show the custom active-account check firing.
    private static final boolean ACTIVE = true;

    private static final String USERNAME = "dev1";

    // Argon2id hash of "password123", produced by
    // Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().
    // Run com.taskflow.util.PasswordHashGenerator to regenerate this for
    // a different plaintext password, then paste the output here.
    private static final String PASSWORD_HASH =
            "$argon2id$v=19$m=16384,t=2,p=1$jgggKZwaOqNGiisxZV8ZDg$G5N++wpjV2C4l5XfdQkCZdAnUbqAXZ1noKKWyt54QOY";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!USERNAME.equals(username)) {
            throw new UsernameNotFoundException("No user found for: " + username);
        }
        return new AppUserDetails(USERNAME, PASSWORD_HASH, ACTIVE);
    }
}
