package com.encoretickets.authdemo.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Not this exercise's focus — already implemented for you (see the previous
 * module if you want the deep dive on custom UserDetailsService wiring).
 * The one addition here is .accountLocked(...), which is what makes the
 * lockout you're about to build actually take effect.
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DbUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole())
                .disabled(!user.isEnabled())
                .accountLocked(user.isAccountLocked())
                .build();
    }
}
