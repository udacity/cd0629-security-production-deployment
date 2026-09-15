package com.udabank.orderservice;

import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

/**
 * orders_placed_total: a plain counter, incremented once per successfully
 * handled order request.
 *
 * order_processing: wraps the inventory-check call with the Micrometer
 * Observation API. With actuator + micrometer-registry-prometheus on the
 * classpath, Spring Boot auto-configures a DefaultMeterObservationHandler
 * that turns every Observation into a Timer automatically — no manual
 * Timer.builder(...).record(...) needed. It shows up in
 * /actuator/prometheus as order_processing_seconds (Micrometer appends
 * the unit and converts dots/underscores per the Prometheus naming
 * convention).
 */
@Component
public class OrderMetrics {

    private final Counter ordersPlacedCounter;
    private final ObservationRegistry observationRegistry;

    public OrderMetrics(MeterRegistry meterRegistry, ObservationRegistry observationRegistry) {
        this.ordersPlacedCounter = Counter.builder("orders_placed_total")
                .description("Total number of orders successfully placed")
                .register(meterRegistry);
        this.observationRegistry = observationRegistry;
    }

    public void recordOrderPlaced() {
        ordersPlacedCounter.increment();
    }

    public <T> T timeOrderProcessing(Supplier<T> block) {
        return Observation.createNotStarted("order_processing", observationRegistry).observe(block);
    }
}
