package com.udabank.authdemo.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Task (b): Load users from the `users` table instead of an in-memory map.
 *
 * TODO:
 *   1. Inject UserRepository (constructor injection).
 *   2. Implement loadUserByUsername(username):
 *      - Look the user up via userRepository.findByUsername(username).
 *      - If not found, throw UsernameNotFoundException.
 *      - If found, map the User entity to a Spring Security UserDetails object:
 *          org.springframework.security.core.userdetails.User.builder()
 *              .username(...)
 *              .password(...)          // already-hashed password from the DB
 *              .authorities(...)        // "ROLE_" + user.getRole()
 *              .disabled(!user.isEnabled())
 *              .build();
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    // TODO: add a UserRepository field + constructor

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: replace this stub with a real DB-backed lookup
        throw new UnsupportedOperationException("DbUserDetailsService.loadUserByUsername not implemented yet");
    }
}
