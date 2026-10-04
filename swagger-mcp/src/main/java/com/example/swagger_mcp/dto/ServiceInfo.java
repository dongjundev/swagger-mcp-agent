package com.example.swagger_mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceInfo(
        String serviceName,
        String title,
        String description,
        String version,
        List<String> servers,
        int apiCount,
        Instant fetchedAt
) {
}
