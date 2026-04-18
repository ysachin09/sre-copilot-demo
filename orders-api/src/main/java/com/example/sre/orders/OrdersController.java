package com.example.sre.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/orders")
class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    @PostMapping
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        String orderId = "ord_" + UUID.randomUUID().toString().substring(0, 8);
        long latencyMs = 80 + ThreadLocalRandom.current().nextLong(200);
        log.info("Order created: order_id={} customer_id={} latency_ms={}", orderId, body.get("customerId"), latencyMs);
        return Map.of("orderId", orderId, "status", "ACCEPTED");
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        log.info("Order lookup: order_id={}", orderId);
        return Map.of("orderId", orderId, "status", "FULFILLED");
    }
}
