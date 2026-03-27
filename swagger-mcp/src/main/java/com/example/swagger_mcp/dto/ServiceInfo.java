package com.example.swagger_mcp.dto;

import java.time.Instant;

public record ServiceInfo(
        String serviceName,
        String version,
        int apiCount,
        Instant registeredAt
) {
}
