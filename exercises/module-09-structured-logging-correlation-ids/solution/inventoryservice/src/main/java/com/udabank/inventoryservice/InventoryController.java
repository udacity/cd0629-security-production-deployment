package com.udabank.inventoryservice;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @GetMapping("/inventory/{sku}")
    public Map<String, Object> checkStock(@PathVariable String sku) {
        log.info("Checking inventory for sku={}", sku);
        return Map.of("sku", sku, "quantity", 42);
    }
}
