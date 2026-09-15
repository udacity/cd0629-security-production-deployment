package com.udabank.authdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Given — not part of the exercise. Binds to whatever "db.username" /
 * "db.password" resolve to at startup, regardless of which property
 * source they come from (application.yml today; Vault after your fix).
 */
@ConfigurationProperties(prefix = "db")
public class DatabaseCredentials {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
