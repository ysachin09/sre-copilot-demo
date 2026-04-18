package com.example.sre.mcp.service;

import com.example.sre.mcp.config.ServiceDefinition;
import com.example.sre.mcp.model.HealthStatus;
import com.example.sre.mcp.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
public class HealthChecker {

    private static final Logger log = LoggerFactory.getLogger(HealthChecker.class);
    private static final int HEALTH_WINDOW_MINUTES = 5;
    private static final double DEGRADED_THRESHOLD = 0.05;

    private final RestClient restClient;
    private final LogReader logReader;

    public HealthChecker(RestClient restClient, LogReader logReader) {
        this.restClient = restClient;
        this.logReader = logReader;
    }

    public HealthStatus check(ServiceDefinition svc) {
        boolean reachable = isReachable(svc);
        if (!reachable) {
            return new HealthStatus(svc.name(), "DOWN", 0.0, 0, 0,
                    String.valueOf(HEALTH_WINDOW_MINUTES), null, Instant.now().toString());
        }

        List<LogEntry> window;
        try {
            window = logReader.inWindow(Path.of(svc.logFile()), HEALTH_WINDOW_MINUTES);
        } catch (Exception e) {
            log.warn("Could not read log file for {}: {}", svc.name(), e.getMessage());
            return new HealthStatus(svc.name(), "UP_HEALTHY", 0.0, 0, 0,
                    String.valueOf(HEALTH_WINDOW_MINUTES), null, Instant.now().toString());
        }

        int total = window.size();
        long errors = window.stream().filter(e -> "ERROR".equals(e.level())).count();
        String lastError = window.stream()
                .filter(e -> "ERROR".equals(e.level()))
                .reduce((a, b) -> b)
                .map(LogEntry::message)
                .orElse(null);

        double errorRate = total > 0 ? (double) errors / total : 0.0;
        String status = errorRate >= DEGRADED_THRESHOLD ? "UP_DEGRADED" : "UP_HEALTHY";

        return new HealthStatus(svc.name(), status, errorRate, (int) errors, total,
                String.valueOf(HEALTH_WINDOW_MINUTES), lastError, Instant.now().toString());
    }

    public boolean isReachable(ServiceDefinition svc) {
        try {
            restClient.get()
                    .uri(svc.baseUrl() + "/actuator/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
