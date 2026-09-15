package com.udabank.orderservice;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "placed_orders")
public class PlacedOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String sku;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlacedOrder() {
        // JPA
    }

    public PlacedOrder(String orderId, String sku) {
        this.orderId = orderId;
        this.sku = sku;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
