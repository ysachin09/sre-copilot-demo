package com.example.sre.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/inventory")
class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @GetMapping("/{productId}")
    public Map<String, Object> getStock(@PathVariable String productId) {
        int quantity = ThreadLocalRandom.current().nextInt(0, 500);
        log.info("Stock query: product_id={} quantity={}", productId, quantity);
        return Map.of("productId", productId, "quantity", quantity, "warehouse", "us-east");
    }

    @PostMapping("/{productId}/reserve")
    public Map<String, Object> reserve(@PathVariable String productId, @RequestBody Map<String, Object> body) {
        int requested = (int) body.getOrDefault("quantity", 1);
        log.info("Stock reserved: product_id={} quantity={}", productId, requested);
        return Map.of("productId", productId, "reserved", requested, "status", "OK");
    }
}
