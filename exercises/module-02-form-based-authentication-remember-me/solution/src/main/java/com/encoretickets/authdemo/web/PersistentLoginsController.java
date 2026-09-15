package com.encoretickets.authdemo.web;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solution walkthrough: "Inspecting persistent_logins entries".
 *
 * ADMIN-only (see SecurityConfig's /admin/** rule) debug view over the table
 * JdbcTokenRepositoryImpl auto-creates. Handy for confirming a remember-me
 * login actually persisted a token instead of just trusting the cookie was
 * set — the series/token pair is what gets validated on the next visit.
 */
@RestController
public class PersistentLoginsController {

    private final JdbcTemplate jdbcTemplate;

    public PersistentLoginsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/persistent-logins")
    public List<Map<String, Object>> persistentLogins() {
        return jdbcTemplate.queryForList(
                "SELECT username, series, last_used FROM persistent_logins ORDER BY last_used DESC");
    }
}
