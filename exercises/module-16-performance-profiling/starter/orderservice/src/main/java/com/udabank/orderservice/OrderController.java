package com.udabank.orderservice;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * The endpoint this exercise's load test hits — "list my orders with
     * their line items," the kind of endpoint a real order-history page
     * would call. See OrderService for the bottleneck.
     */
    @GetMapping("/orders")
    public List<OrderSummary> listOrders() {
        return orderService.listOrdersWithItems();
    }
}
