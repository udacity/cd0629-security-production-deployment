package com.udabank.orderservice;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlacedOrderRepository extends JpaRepository<PlacedOrder, Long> {

    // Collapses the N+1 (findAll() + one lazy-load per order) into one
    // query. DISTINCT matters — without it, the join produces one row
    // per line item, so an order with 3 items appears 3 times before
    // Hibernate's entity-identity de-duplication kicks in.
    @Query("SELECT DISTINCT o FROM PlacedOrder o LEFT JOIN FETCH o.lineItems")
    List<PlacedOrder> findAllWithLineItems();
}
