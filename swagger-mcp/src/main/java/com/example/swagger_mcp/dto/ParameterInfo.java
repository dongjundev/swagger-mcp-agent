package com.example.swagger_mcp.dto;

import java.util.Map;

public record ParameterInfo(
        String name,
        String in,
        boolean required,
        String description,
        Map<String, Object> schema
) {
}
