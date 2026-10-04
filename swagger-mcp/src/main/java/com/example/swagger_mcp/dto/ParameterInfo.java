package com.example.swagger_mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParameterInfo(
        String name,
        String in,
        boolean required,
        String description,
        Map<String, Object> schema
) {
}
