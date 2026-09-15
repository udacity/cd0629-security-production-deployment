package com.udabank.orderservice;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The RestClient here already has TraceparentPropagatingInterceptor
 * registered, so every call automatically propagates the current trace.
 *
 * checkStock is @Cacheable against Redis (see application.yml — cache
 * name "inventory", 30s TTL) since inventory lookups for the same SKU are
 * read far more often than stock actually changes.
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
    @Cacheable("inventory")
    public Map<String, Object> checkStock(String sku) {
        return restClient.get()
                .uri("/inventory/{sku}", sku)
                .retrieve()
                .body(Map.class);
    }
}
