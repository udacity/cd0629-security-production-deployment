package com.taskflow.controller;

import com.taskflow.config.PartnerApiKeyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

/**
 * Module 13 solution: outgoing requests are restricted to an explicit
 * allowlist of trusted domains, matching Module 12's exact example.
 * Never let raw, unvalidated user input dictate outgoing server
 * requests — check the destination before the request ever leaves.
 *
 * Module 15: real partner API calls need to authenticate themselves,
 * so a real API key is attached to every outgoing request now.
 *
 * Module 19 solution: the debug log line from the starter state is
 * fixed, not removed — a log line confirming a key was attached is
 * genuinely useful. What changed is what it prints: a masked value,
 * never the raw secret.
 */
@RestController
public class PartnerController {

    private static final Logger log = LoggerFactory.getLogger(PartnerController.class);

    private static final List<String> ALLOWED_HOSTS = List.of(
            "trusted-partner.com",
            "api-udapay.com"
    );

    private final RestClient restClient = RestClient.create();
    private final PartnerApiKeyProperties partnerApiKeyProperties;

    public PartnerController(PartnerApiKeyProperties partnerApiKeyProperties) {
        this.partnerApiKeyProperties = partnerApiKeyProperties;
    }

    @GetMapping("/api/v1/partners/fetch")
    public String fetch(@RequestParam String url) {
        String host = URI.create(url).getHost();
        if (host == null || !ALLOWED_HOSTS.contains(host)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Host not on the allowlist: " + host);
        }

        log.info("Attaching partner API key ending in: {}", maskKey(partnerApiKeyProperties.getApiKey()));

        return restClient.get()
                .uri(url)
                .header("X-Partner-Api-Key", partnerApiKeyProperties.getApiKey())
                .retrieve()
                .body(String.class);
    }

    private String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}


