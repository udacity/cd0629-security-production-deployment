package com.taskflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * DELIBERATELY VULNERABLE — Module 13's starter state.
 *
 * Stands in for a real feature: fetching data from a partner service on
 * a user's behalf. Fetches whatever URL it's given, no validation. A
 * malicious caller can use this server as a proxy to reach internal
 * infrastructure the server can see but the caller never could directly
 * (classic SSRF: cloud metadata endpoints, internal admin panels, etc.).
 */
@RestController
public class PartnerController {

    private final RestClient restClient = RestClient.create();

    @GetMapping("/api/v1/partners/fetch")
    public String fetch(@RequestParam String url) {
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
}
