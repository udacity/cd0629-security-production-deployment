package com.udabank.orderservice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacedOrderRepository extends JpaRepository<PlacedOrder, Long> {
}
