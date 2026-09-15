package com.taskflow.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * A plain UserDetails implementation with two additions: `active` and,
 * as of Module 7, `role`.
 *
 * On purpose, this does NOT wire `active` into isEnabled(). The whole
 * point of Demo 3 is showing a security rule that lives in OUR OWN
 * AuthenticationProvider, not one borrowed from Spring's built-in
 * account-status hooks. isEnabled() below always returns true; the
 * active check happens explicitly in CustomAuthenticationProvider.
 */
public class AppUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final boolean active;
    private final String role;

    public AppUserDetails(String username, String password, boolean active, String role) {
        this.username = username;
        this.password = password;
        this.active = active;
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
