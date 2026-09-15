package com.udabank.orderservice;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final InventoryClient inventoryClient;

    public OrderController(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        log.info("Handling order request for orderId={}", orderId);
        Map<String, Object> stock = inventoryClient.checkStock("SKU-123");
        return Map.of("orderId", orderId, "item", stock);
    }

    /**
     * Deliberately broken — surcharge-service integration was never
     * finished. This is the "simulated incident" for the dashboard
     * exercise: hit this a bunch of times and watch the error rate spike
     * in Kibana. See docs/EXERCISE.md, Exercise Step 6.
     */
    @GetMapping("/orders/{orderId}/expedite")
    public Map<String, Object> expedite(@PathVariable String orderId) {
        log.info("Handling expedite request for orderId={}", orderId);
        log.error("Expedited shipping surcharge lookup failed for orderId={}: surcharge-service unreachable", orderId);
        throw new IllegalStateException("surcharge-service unreachable");
    }
}
