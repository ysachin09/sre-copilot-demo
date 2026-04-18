package com.example.sre.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
class OrdersLogSimulator {

    private static final Logger log = LoggerFactory.getLogger(OrdersLogSimulator.class);

    private static final List<String> CUSTOMERS = List.of(
            "cust_alpha", "cust_beta", "cust_gamma", "cust_delta", "cust_epsilon"
    );
    private static final List<String> PRODUCTS = List.of(
            "prod_widget", "prod_gadget", "prod_doohickey", "prod_thingamajig"
    );

    private final AtomicInteger orderCounter = new AtomicInteger(10000);

    @Scheduled(fixedRate = 8000)
    public void simulateOrderFlow() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String orderId = "ord_" + orderCounter.incrementAndGet();
        String customerId = CUSTOMERS.get(rng.nextInt(CUSTOMERS.size()));
        String productId = PRODUCTS.get(rng.nextInt(PRODUCTS.size()));
        int quantity = rng.nextInt(1, 10);
        long latencyMs = 60 + rng.nextLong(150);

        log.info("Order received: order_id={} customer_id={} product_id={} quantity={}", orderId, customerId, productId, quantity);

        if (rng.nextDouble() < 0.08) {
            long slowMs = 2000 + rng.nextLong(1500);
            log.warn("Order processing slow: order_id={} fulfillment_ms={} threshold_ms=2000", orderId, slowMs);
        } else {
            log.info("Order fulfilled: order_id={} fulfillment_ms={} customer_id={}", orderId, latencyMs, customerId);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void logQueueDepth() {
        int depth = ThreadLocalRandom.current().nextInt(0, 12);
        if (depth > 8) {
            log.warn("Order queue depth elevated: depth={} threshold=8", depth);
        } else {
            log.info("Order queue healthy: depth={}", depth);
        }
    }
}
