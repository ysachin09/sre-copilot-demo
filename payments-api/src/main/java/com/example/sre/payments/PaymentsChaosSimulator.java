package com.example.sre.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
class PaymentsChaosSimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentsChaosSimulator.class);

    private static final List<String> MERCHANTS = List.of(
            "acme_corp", "globex_inc", "initech_llc", "umbrella_co"
    );
    private static final List<String> FAILURE_CAUSES = List.of(
            "GATEWAY_TIMEOUT", "CONNECTION_REFUSED", "SSL_HANDSHAKE_FAILED", "RATE_LIMITED"
    );

    private final ChaosState chaosState;

    PaymentsChaosSimulator(ChaosState chaosState) {
        this.chaosState = chaosState;
    }

    @Scheduled(fixedRate = 10000)
    public void simulatePaymentAttempt() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String txId = "tx_" + UUID.randomUUID().toString().substring(0, 8);
        String merchant = MERCHANTS.get(rng.nextInt(MERCHANTS.size()));
        double amount = 10.0 + rng.nextDouble(990.0);
        String amountStr = String.format("%.2f", amount);

        log.info("Payment initiated: tx_id={} merchant={} amount={}", txId, merchant, amountStr);

        if (rng.nextDouble() < chaosState.failureRate()) {
            String cause = FAILURE_CAUSES.get(rng.nextInt(FAILURE_CAUSES.size()));
            long timeoutMs = 3000 + rng.nextLong(3000);
            log.error("Payment gateway error: tx_id={} cause={} timeout_ms={} merchant={}", txId, cause, timeoutMs, merchant);
            log.error("Payment transaction failed: tx_id={} amount={} merchant={} error={}", txId, amountStr, merchant, cause);
        } else {
            long latencyMs = 120 + rng.nextLong(300);
            log.info("Payment processed: tx_id={} merchant={} amount={} latency_ms={}", txId, merchant, amountStr, latencyMs);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void logGatewayStatus() {
        double rate = chaosState.failureRate();
        String rateStr = String.format("%.0f%%", rate * 100);
        if (rate > 0.1) {
            log.error("Payment gateway health check degraded: error_rate={} threshold=10%", rateStr);
        } else if (rate > 0.0) {
            log.warn("Payment gateway health check marginal: error_rate={}", rateStr);
        } else {
            log.info("Payment gateway health check OK: error_rate=0%");
        }
    }
}
