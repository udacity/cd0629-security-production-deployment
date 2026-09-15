package com.udabank.authdemo;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Given — not part of the exercise. Reports whether credentials resolved
 * at startup, without ever exposing the actual password.
 */
@RestController
public class DbStatusController {

    private final DatabaseCredentials databaseCredentials;

    public DbStatusController(DatabaseCredentials databaseCredentials) {
        this.databaseCredentials = databaseCredentials;
    }

    @GetMapping("/internal/db-status")
    public Map<String, Object> status() {
        return Map.of(
                "username", databaseCredentials.getUsername(),
                "passwordConfigured", databaseCredentials.getPassword() != null
                        && !databaseCredentials.getPassword().isBlank());
    }
}
