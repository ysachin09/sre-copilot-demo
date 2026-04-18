package com.example.sre.mcp.tools;

import com.example.sre.mcp.config.ServiceDefinition;
import com.example.sre.mcp.config.ServiceRegistry;
import com.example.sre.mcp.model.ServiceSummary;
import com.example.sre.mcp.service.HealthChecker;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceDiscoveryTool {

    private final ServiceRegistry registry;
    private final HealthChecker healthChecker;

    public ServiceDiscoveryTool(ServiceRegistry registry, HealthChecker healthChecker) {
        this.registry = registry;
        this.healthChecker = healthChecker;
    }

    @Tool(description = """
            List all registered services in the SRE environment with their current reachability status.
            Returns service name, base URL, and whether the service is currently responding to health checks.
            Use this as the entry point to discover what services exist before querying logs or health.
            """)
    public List<ServiceSummary> list_services() {
        return registry.all().stream()
                .map(svc -> new ServiceSummary(
                        svc.name(),
                        svc.baseUrl(),
                        healthChecker.isReachable(svc) ? "UP" : "DOWN"
                ))
                .toList();
    }
}
