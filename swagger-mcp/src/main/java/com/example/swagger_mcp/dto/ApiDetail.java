package com.example.swagger_mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiDetail(
        String operationId,
        String httpMethod,
        String path,
        String summary,
        String description,
        List<ParameterInfo> parameters,
        Map<String, Object> requestBody,
        Map<String, Object> responses,
        List<Map<String, List<String>>> security,
        Map<String, Object> securitySchemes
) {
}
