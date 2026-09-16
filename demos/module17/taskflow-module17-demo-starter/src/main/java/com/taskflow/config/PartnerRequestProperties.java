package com.taskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Module 17: a plain operational setting, not a secret. Deliberately
 * contrasted with PartnerApiKeyProperties (Module 15) — the API key
 * needed Vault, because it verifies identity. A request timeout is just
 * a number; Module 16 was explicit that these two categories shouldn't
 * be mixed. This one is a good candidate for centralized, version
 * controlled config instead.
 */
@Component
@ConfigurationProperties(prefix = "partner.request")
public class PartnerRequestProperties {

    private int timeoutSeconds;

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
