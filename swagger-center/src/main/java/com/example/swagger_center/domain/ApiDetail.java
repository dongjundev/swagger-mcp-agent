package com.example.swagger_center.domain;

import java.util.List;
import java.util.Map;

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
