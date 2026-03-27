package com.example.swagger_mcp.dto;

import java.util.List;
import java.util.Map;

public record ApiDetail(
        String operationId,
        String httpMethod,
        String path,
        String summary,
        List<ParameterInfo> parameters,
        Map<String, Object> requestBody,
        Map<String, Object> responses
) {
}
