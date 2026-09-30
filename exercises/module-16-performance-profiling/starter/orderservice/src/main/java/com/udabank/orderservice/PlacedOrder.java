package com.udabank.orderservice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    // LAZY (the JPA default for @OneToMany) is exactly what makes the N+1
    // possible: nothing loads these until something calls getLineItems(),
    // at which point Hibernate issues a fresh SELECT for THIS order's
    // items alone — fine for one order, one extra query per order when
    // you're listing all of them.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderLineItem> lineItems = new ArrayList<>();

    protected PlacedOrder() {
        // JPA
    }

    public PlacedOrder(String orderId, String sku) {
        this.orderId = orderId;
        this.sku = sku;
        this.createdAt = Instant.now();
    }

    public void addLineItem(OrderLineItem item) {
        lineItems.add(item);
        item.setOrder(this);
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

    public List<OrderLineItem> getLineItems() {
        return lineItems;
    }
}
