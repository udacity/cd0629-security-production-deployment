package com.udabank.orderservice;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Given — not part of the exercise. Whatever "welcome.message" resolves
 * to (from application.yml today; from the config server after your fix)
 * shows up here.
 */
@RestController
public class WelcomeController {

    @Value("${welcome.message}")
    private String welcomeMessage;

    @GetMapping("/message")
    public Map<String, String> message() {
        return Map.of("message", welcomeMessage);
    }
}
