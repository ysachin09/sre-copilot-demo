package com.example.sre.mcp.model;

public record HealthStatus(
        String service,
        String status,
        double errorRate,
        int errorCount,
        int totalCount,
        String windowMinutes,
        String lastErrorMessage,
        String checkedAt
) {}
