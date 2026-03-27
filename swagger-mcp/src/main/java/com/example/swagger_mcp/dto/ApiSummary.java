package com.example.swagger_mcp.dto;

public record ApiSummary(
        String operationId,
        String httpMethod,
        String path,
        String summary
) {
}
