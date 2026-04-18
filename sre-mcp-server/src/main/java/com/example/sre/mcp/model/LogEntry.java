package com.example.sre.mcp.model;

public record LogEntry(
        String timestamp,
        String level,
        String message,
        String service,
        String logger
) {}
