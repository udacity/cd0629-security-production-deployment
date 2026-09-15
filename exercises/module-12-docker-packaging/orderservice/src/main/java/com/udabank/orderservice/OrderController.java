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

    private static final String SKU = "SKU-123";

    private final InventoryClient inventoryClient;
    private final OrderMetrics orderMetrics;
    private final PlacedOrderRepository placedOrderRepository;

    public OrderController(InventoryClient inventoryClient, OrderMetrics orderMetrics,
                            PlacedOrderRepository placedOrderRepository) {
        this.inventoryClient = inventoryClient;
        this.orderMetrics = orderMetrics;
        this.placedOrderRepository = placedOrderRepository;
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        log.info("Handling order request for orderId={}", orderId);
        Map<String, Object> stock = orderMetrics.timeOrderProcessing(() -> inventoryClient.checkStock(SKU));
        orderMetrics.recordOrderPlaced();
        placedOrderRepository.save(new PlacedOrder(orderId, SKU));
        return Map.of("orderId", orderId, "item", stock);
    }
}
