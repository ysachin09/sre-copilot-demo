package com.example.sre.mcp.config;

import com.example.sre.mcp.tools.HealthTool;
import com.example.sre.mcp.tools.LogQueryTool;
import com.example.sre.mcp.tools.RemediationTool;
import com.example.sre.mcp.tools.ServiceDiscoveryTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class McpToolsConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public ToolCallbackProvider sreTools(
            ServiceDiscoveryTool discovery,
            LogQueryTool logQuery,
            HealthTool health,
            RemediationTool remediation) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(discovery, logQuery, health, remediation)
                .build();
    }
}
