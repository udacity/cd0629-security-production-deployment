package com.udabank.orderservice;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final InventoryClient inventoryClient;
    private final OrderMetrics orderMetrics;

    public OrderController(InventoryClient inventoryClient, OrderMetrics orderMetrics) {
        this.inventoryClient = inventoryClient;
        this.orderMetrics = orderMetrics;
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        log.info("Handling order request for orderId={}", orderId);
        Map<String, Object> stock = orderMetrics.timeOrderProcessing(() -> inventoryClient.checkStock("SKU-123"));
        orderMetrics.recordOrderPlaced();
        return Map.of("orderId", orderId, "item", stock);
    }

    /**
     * Deliberately broken — surcharge-service integration was never
     * finished. Useful for an error-rate-spike demo.
     */
    @GetMapping("/orders/{orderId}/expedite")
    public Map<String, Object> expedite(@PathVariable String orderId) {
        log.info("Handling expedite request for orderId={}", orderId);
        log.error("Expedited shipping surcharge lookup failed for orderId={}: surcharge-service unreachable", orderId);
        throw new IllegalStateException("surcharge-service unreachable");
    }

    /**
     * Deliberately slow (1.5-3s, well past any reasonable p95 SLO) — this
     * is the "simulated incident" for the latency-SLO-burn alert exercise.
     * See README.md, Exercise Step 6.
     */
    @GetMapping("/orders/{orderId}/slow")
    public Map<String, Object> slow(@PathVariable String orderId) throws InterruptedException {
        long delayMs = ThreadLocalRandom.current().nextLong(1500, 3000);
        log.info("Handling slow order request for orderId={}, simulated delay={}ms", orderId, delayMs);
        Map<String, Object> stock = orderMetrics.timeOrderProcessing(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return inventoryClient.checkStock("SKU-123");
        });
        orderMetrics.recordOrderPlaced();
        return Map.of("orderId", orderId, "item", stock);
    }
}
