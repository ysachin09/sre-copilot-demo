package com.example.sre.mcp.tools;

import com.example.sre.mcp.config.ServiceDefinition;
import com.example.sre.mcp.config.ServiceRegistry;
import com.example.sre.mcp.model.LogEntry;
import com.example.sre.mcp.model.ToolError;
import com.example.sre.mcp.service.LogReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class LogQueryTool {

    private static final int MAX_LIMIT = 50;

    private final ServiceRegistry registry;
    private final LogReader logReader;

    public LogQueryTool(ServiceRegistry registry, LogReader logReader) {
        this.registry = registry;
        this.logReader = logReader;
    }

    @Tool(description = """
            Retrieve the most recent log lines for a named service.
            Returns structured log entries (timestamp, level, message) in chronological order.
            Max 50 lines per call. Response includes 'returned' and 'truncated' metadata.
            Supported services: orders-api, payments-api, inventory-api.
            """)
    public Map<String, Object> get_recent_logs(String service, int limit) {
        var validation = validateService(service);
        if (validation != null) return Map.of("error", validation);

        int capped = Math.min(Math.max(1, limit), MAX_LIMIT);
        ServiceDefinition svc = registry.find(service).get();

        try {
            List<LogEntry> entries = logReader.tail(Path.of(svc.logFile()), capped);
            return Map.of(
                    "service", service,
                    "returned", entries.size(),
                    "requestedLimit", limit,
                    "cappedAt", MAX_LIMIT,
                    "truncated", limit > MAX_LIMIT,
                    "entries", entries
            );
        } catch (Exception e) {
            return Map.of("error", ToolError.backendDegraded("Failed to read logs for " + service + ": " + e.getMessage()));
        }
    }

    @Tool(description = """
            Search log lines for a service by keyword or phrase within an optional time window.
            The query is matched case-insensitively against the full log line content.
            timeRangeMinutes: how many minutes back to search (0 = no time filter, max 1440).
            Returns up to 50 matching entries in chronological order.
            """)
    public Map<String, Object> search_logs(String service, String query, int timeRangeMinutes) {
        var validation = validateService(service);
        if (validation != null) return Map.of("error", validation);

        if (query == null || query.isBlank()) {
            return Map.of("error", ToolError.invalidArgument("query must not be empty"));
        }

        int windowMinutes = Math.min(Math.max(0, timeRangeMinutes), 1440);
        ServiceDefinition svc = registry.find(service).get();

        try {
            List<LogEntry> matches = logReader.search(Path.of(svc.logFile()), query, windowMinutes);
            return Map.of(
                    "service", service,
                    "query", query,
                    "timeRangeMinutes", windowMinutes,
                    "returned", matches.size(),
                    "truncated", matches.size() >= 50,
                    "entries", matches
            );
        } catch (Exception e) {
            return Map.of("error", ToolError.backendDegraded("Log search failed for " + service + ": " + e.getMessage()));
        }
    }

    private ToolError validateService(String service) {
        if (service == null || service.isBlank()) {
            return ToolError.invalidArgument("service name must not be empty");
        }
        if (!registry.exists(service)) {
            return ToolError.notFound("Service '" + service + "' is not registered. Known services: "
                    + registry.all().stream().map(ServiceDefinition::name).toList());
        }
        return null;
    }
}
