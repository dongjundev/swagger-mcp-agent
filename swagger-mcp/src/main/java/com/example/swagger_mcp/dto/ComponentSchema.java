package com.example.swagger_mcp.dto;

import java.util.Map;

public record ComponentSchema(
        String name,
        Map<String, Object> schema
) {
}
