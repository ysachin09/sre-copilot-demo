package com.example.sre.mcp.tools;

import com.example.sre.mcp.config.ServiceDefinition;
import com.example.sre.mcp.config.ServiceRegistry;
import com.example.sre.mcp.model.HealthStatus;
import com.example.sre.mcp.model.ToolError;
import com.example.sre.mcp.service.HealthChecker;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HealthTool {

    private final ServiceRegistry registry;
    private final HealthChecker healthChecker;

    public HealthTool(ServiceRegistry registry, HealthChecker healthChecker) {
        this.registry = registry;
        this.healthChecker = healthChecker;
    }

    @Tool(description = """
            Get the health status of a specific service using deterministic heuristics — no LLM involved.
            Status values:
              UP_HEALTHY   — service is reachable and error rate < 5% in the last 5 minutes
              UP_DEGRADED  — service is reachable but error rate >= 5% in the last 5 minutes
              DOWN         — service health endpoint is unreachable
            Also returns: errorRate, errorCount, totalCount, lastErrorMessage, checkedAt.
            """)
    public Map<String, Object> get_service_health(String service) {
        if (service == null || service.isBlank()) {
            return Map.of("error", ToolError.invalidArgument("service name must not be empty"));
        }
        if (!registry.exists(service)) {
            return Map.of("error", ToolError.notFound("Service '" + service + "' is not registered"));
        }

        ServiceDefinition svc = registry.find(service).get();
        HealthStatus status = healthChecker.check(svc);
        return Map.of("health", status);
    }
}
