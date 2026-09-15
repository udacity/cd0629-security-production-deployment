package com.udabank.authdemo.web;

import java.security.Principal;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {

    @GetMapping("/app/dashboard")
    public Map<String, String> dashboard(Principal principal) {
        return Map.of(
                "message", "Ledger dashboard loaded.",
                "authenticatedAs", principal.getName());
    }
}
