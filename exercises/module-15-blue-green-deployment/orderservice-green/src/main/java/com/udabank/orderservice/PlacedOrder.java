package com.udabank.orderservice;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Green (v2) of the expand-contract migration: "sku" is being renamed to
 * "product_sku" for clarity. This version DUAL-WRITES both columns on
 * every insert — "sku" stays populated so blue (still possibly serving
 * traffic, or serving it again after a rollback) keeps working
 * unmodified, while "product_sku" starts getting populated for the
 * eventual v3 that will read from it exclusively.
 *
 * Requires V2 (add product_sku column) to have already run — see
 * db/migration/ — before this version is deployed, or every insert here
 * fails with "column product_sku does not exist."
 */
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

    @Column(name = "product_sku")
    private String productSku;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlacedOrder() {
        // JPA
    }

    public PlacedOrder(String orderId, String sku) {
        this.orderId = orderId;
        this.sku = sku;
        this.productSku = sku; // dual-write: same value into both columns
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

    public String getProductSku() {
        return productSku;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
