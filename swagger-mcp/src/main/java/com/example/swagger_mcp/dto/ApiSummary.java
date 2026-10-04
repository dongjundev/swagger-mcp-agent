package com.example.swagger_mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiSummary(
        String operationId,
        String httpMethod,
        String path,
        String summary,
        List<String> tags,
        Boolean deprecated
) {
}
