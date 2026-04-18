package com.example.sre.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
class InventoryLogSimulator {

    private static final Logger log = LoggerFactory.getLogger(InventoryLogSimulator.class);

    private static final List<String> PRODUCTS = List.of(
            "prod_widget_a", "prod_gadget_b", "prod_doohickey_c",
            "prod_thingamajig_d", "prod_whatsit_e", "prod_gizmo_f"
    );
    private static final List<String> WAREHOUSES = List.of("us-east", "us-west", "eu-central");

    @Scheduled(fixedRate = 12000)
    public void simulateStockChecks() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String productId = PRODUCTS.get(rng.nextInt(PRODUCTS.size()));
        String warehouse = WAREHOUSES.get(rng.nextInt(WAREHOUSES.size()));
        int quantity = rng.nextInt(0, 600);

        if (quantity < 10) {
            log.warn("Low stock alert: product_id={} quantity={} warehouse={} reorder_threshold=10", productId, quantity, warehouse);
        } else {
            log.info("Stock check OK: product_id={} quantity={} warehouse={}", productId, quantity, warehouse);
        }
    }

    @Scheduled(fixedRate = 45000)
    public void simulateSyncJob() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int synced = rng.nextInt(50, 300);
        long durationMs = 200 + rng.nextLong(800);
        log.info("Inventory sync complete: records_synced={} duration_ms={}", synced, durationMs);
    }

    @Scheduled(fixedRate = 90000)
    public void simulateReorderCheck() {
        int belowThreshold = ThreadLocalRandom.current().nextInt(0, 5);
        if (belowThreshold > 0) {
            log.warn("Reorder check: {} product(s) below reorder threshold", belowThreshold);
        } else {
            log.info("Reorder check: all products above threshold");
        }
    }
}
