package com.udabank.orderservice;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is the bottleneck this exercise exists to find and fix. See
 * README.md — don't fix it before you've actually profiled it and seen
 * the real evidence; that's the point of the exercise.
 */
@Service
public class OrderService {

    private final PlacedOrderRepository placedOrderRepository;

    public OrderService(PlacedOrderRepository placedOrderRepository) {
        this.placedOrderRepository = placedOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> listOrdersWithItems() {
        // findAll() -> 1 query for every order. Then, for EACH order,
        // order.getLineItems() below triggers ANOTHER query (lineItems is
        // FetchType.LAZY on PlacedOrder) — 1 + N queries total for N orders.
        return placedOrderRepository.findAllWithLineItems().stream()
                .map(this::toSummary)
                .toList();
    }

    private OrderSummary toSummary(PlacedOrder order) {
        List<String> items = order.getLineItems().stream()
                .map(li -> li.getProductName() + " x" + li.getQuantity())
                .toList();
        return new OrderSummary(order.getOrderId(), order.getSku(), items);
    }
}
