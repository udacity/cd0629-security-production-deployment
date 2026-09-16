package com.taskflow.controller;

import com.taskflow.config.PartnerApiKeyProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
@RestController
public class PartnerController {

    private static final List<String> ALLOWED_HOSTS = List.of(
            "trusted-partner.com",
            "api-udapay.com"
    );

    private final RestClient restClient = RestClient.create();
    private final PartnerApiKeyProperties partnerApiKeyProperties;
    private static final Logger log = LoggerFactory.getLogger(PartnerController.class);

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

        log.info("Attaching partner API key: {}", partnerApiKeyProperties.getApiKey());

        return restClient.get()
                .uri(url)
                .header("X-Partner-Api-Key", partnerApiKeyProperties.getApiKey())
                .retrieve()
                .body(String.class);
    }
}

