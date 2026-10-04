package com.example.swagger_center.domain;

import java.util.List;

public record ApiSummary(
        String operationId,
        String httpMethod,
        String path,
        String summary,
        List<String> tags,
        Boolean deprecated
) {
}
