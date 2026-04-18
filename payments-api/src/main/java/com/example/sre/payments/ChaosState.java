package com.example.sre.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
class ChaosState {

    private final double configuredFailureRate;
    private final AtomicBoolean chaosEnabled = new AtomicBoolean(true);
    private final AtomicReference<String> lastResetReason = new AtomicReference<>("startup");

    ChaosState(@Value("${payments.chaos.failure-rate:0.30}") double failureRate) {
        this.configuredFailureRate = failureRate;
    }

    double failureRate() {
        return chaosEnabled.get() ? configuredFailureRate : 0.0;
    }

    void reset() {
        chaosEnabled.set(false);
        lastResetReason.set("admin-restart");
    }

    String lastResetReason() {
        return lastResetReason.get();
    }
}
