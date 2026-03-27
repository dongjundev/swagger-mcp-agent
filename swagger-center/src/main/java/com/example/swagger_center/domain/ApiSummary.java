package com.example.swagger_center.domain;

public record ApiSummary(
        String operationId,
        String httpMethod,
        String path,
        String summary
) {
}
