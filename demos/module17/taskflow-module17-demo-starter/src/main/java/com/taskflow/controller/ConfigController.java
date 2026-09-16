package com.taskflow.controller;

import com.taskflow.config.PartnerRequestProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Module 17: exists purely so partner.request.timeout-seconds is easy
 * to observe changing, on camera, without needing to actually simulate
 * a slow partner connection timing out.
 */
@RestController
public class ConfigController {

    private final PartnerRequestProperties partnerRequestProperties;

    public ConfigController(PartnerRequestProperties partnerRequestProperties) {
        this.partnerRequestProperties = partnerRequestProperties;
    }

    @GetMapping("/api/v1/config/partner-timeout")
    public Map<String, Object> partnerTimeout() {
        return Map.of("timeoutSeconds", partnerRequestProperties.getTimeoutSeconds());
    }
}
