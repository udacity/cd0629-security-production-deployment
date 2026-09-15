package com.udabank.authdemo.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/public/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Welcome to Udabank — no login required here.");
    }
}
