package com.encoretickets.authdemo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled;

    /** Simple single-role model: "ADMIN" or "USER". Prefixed with ROLE_ when building authorities. */
    @Column(nullable = false)
    private String role;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked;

    protected User() {
        // JPA
    }

    public User(String username, String password, boolean enabled, String role) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.role = role;
        this.failedAttempts = 0;
        this.accountLocked = false;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getRole() {
        return role;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public void lock() {
        this.accountLocked = true;
    }
}
