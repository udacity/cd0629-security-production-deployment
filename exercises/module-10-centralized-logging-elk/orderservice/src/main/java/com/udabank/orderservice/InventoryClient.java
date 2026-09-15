package com.udabank.orderservice;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Given — not part of the exercise. The RestClient here already has
 * TraceparentPropagatingInterceptor registered; once you implement that
 * interceptor's TODO, every call made through this client automatically
 * propagates the current trace.
 */
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder, @Value("${inventory.base-url}") String baseUrl) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestInterceptor(new TraceparentPropagatingInterceptor())
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkStock(String sku) {
        return restClient.get()
                .uri("/inventory/{sku}", sku)
                .retrieve()
                .body(Map.class);
    }
}
