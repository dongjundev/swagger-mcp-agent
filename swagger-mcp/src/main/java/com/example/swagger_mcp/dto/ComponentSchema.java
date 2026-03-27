package com.example.swagger_mcp.dto;

import java.util.List;
import java.util.Map;

public record ComponentSchema(
        String name,
        String type,
        Map<String, Object> properties,
        List<String> required
) {
}
