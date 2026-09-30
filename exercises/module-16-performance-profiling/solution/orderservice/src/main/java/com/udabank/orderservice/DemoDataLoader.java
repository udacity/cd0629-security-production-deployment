package com.udabank.orderservice;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Given — not part of the exercise. Seeds enough data (500 orders, 2-4
 * line items each) that the N+1 in OrderService is actually visible under
 * load rather than theoretical.
 */
@Component
public class DemoDataLoader implements CommandLineRunner {

    private static final String[] PRODUCTS = {
            "Ledger Notebook", "Brass Letter Opener", "Desk Lamp", "Wireless Mouse", "Coffee Mug"
    };

    private final PlacedOrderRepository placedOrderRepository;

    public DemoDataLoader(PlacedOrderRepository placedOrderRepository) {
        this.placedOrderRepository = placedOrderRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 1; i <= 500; i++) {
            PlacedOrder order = new PlacedOrder("ORD-" + i, "SKU-" + (100 + i % 20));
            int itemCount = random.nextInt(2, 5);
            for (int j = 0; j < itemCount; j++) {
                String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
                order.addLineItem(new OrderLineItem(product, random.nextInt(1, 4), random.nextInt(500, 5000)));
            }
            placedOrderRepository.save(order);
        }
    }
}
