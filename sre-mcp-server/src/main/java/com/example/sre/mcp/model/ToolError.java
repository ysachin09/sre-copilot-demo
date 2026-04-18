package com.example.sre.mcp.model;

public record ToolError(String category, String message, boolean retryable) {

    public static ToolError notFound(String msg) {
        return new ToolError("not_found", msg, false);
    }

    public static ToolError invalidArgument(String msg) {
        return new ToolError("invalid_argument", msg, false);
    }

    public static ToolError backendDegraded(String msg) {
        return new ToolError("backend_degraded", msg, true);
    }

    public static ToolError internal(String msg) {
        return new ToolError("internal_error", msg, false);
    }
}
