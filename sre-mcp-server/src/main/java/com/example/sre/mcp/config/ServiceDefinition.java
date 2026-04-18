package com.example.sre.mcp.config;

public record ServiceDefinition(
        String name,
        String baseUrl,
        String logFile,
        String restartEndpoint
) {}
