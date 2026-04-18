package com.example.sre.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final ChaosState chaosState;

    AdminController(ChaosState chaosState) {
        this.chaosState = chaosState;
    }

    @PostMapping("/restart")
    public Map<String, String> restart() {
        chaosState.reset();
        log.info("Payments service restart requested via admin endpoint — chaos state cleared, error injection disabled");
        return Map.of(
                "status", "restarted",
                "service", "payments-api",
                "message", "Service state reset — error injection disabled"
        );
    }
}
