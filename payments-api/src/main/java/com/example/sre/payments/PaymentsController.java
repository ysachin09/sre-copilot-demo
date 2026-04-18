package com.example.sre.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/payments")
class PaymentsController {

    private static final Logger log = LoggerFactory.getLogger(PaymentsController.class);
    private final ChaosState chaosState;

    PaymentsController(ChaosState chaosState) {
        this.chaosState = chaosState;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> body) {
        String txId = "tx_" + UUID.randomUUID().toString().substring(0, 8);
        if (ThreadLocalRandom.current().nextDouble() < chaosState.failureRate()) {
            log.error("Payment processing failed on HTTP endpoint: tx_id={} cause=GATEWAY_TIMEOUT", txId);
            return ResponseEntity.status(503).body(Map.of("txId", txId, "error", "GATEWAY_TIMEOUT"));
        }
        log.info("Payment processed via HTTP: tx_id={}", txId);
        return ResponseEntity.ok(Map.of("txId", txId, "status", "SUCCESS"));
    }
}
