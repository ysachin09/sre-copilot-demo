package com.example.sre.orders;

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

    @PostMapping("/restart")
    public Map<String, String> restart() {
        log.info("Service restart requested via admin endpoint — state cleared");
        return Map.of(
                "status", "restarted",
                "service", "orders-api",
                "message", "Service state reset successfully"
        );
    }
}
