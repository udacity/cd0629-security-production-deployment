package com.taskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Module 15: this class never changes between the vulnerable starter
 * state and the fixed Vault-backed state. That's the whole point Module
 * 14 promised — once a value looks like any other Spring property, your
 * code can't tell whether it came from application.yml or from Vault.
 */
@Component
@ConfigurationProperties(prefix = "partner")
public class PartnerApiKeyProperties {

    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
