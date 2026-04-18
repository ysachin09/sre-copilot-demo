package com.example.sre.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "sre")
public class ServiceRegistry {

    private List<ServiceDefinition> services = List.of();

    public void setServices(List<ServiceDefinition> services) {
        Map<String, ServiceDefinition> map = new LinkedHashMap<>();
        services.forEach(s -> map.put(s.name(), s));
        this.serviceMap = map;
        this.services = services;
    }

    private Map<String, ServiceDefinition> serviceMap = new LinkedHashMap<>();

    public Optional<ServiceDefinition> find(String name) {
        return Optional.ofNullable(serviceMap.get(name));
    }

    public List<ServiceDefinition> all() {
        return services;
    }

    public boolean exists(String name) {
        return serviceMap.containsKey(name);
    }
}
