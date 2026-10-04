package com.example.swagger_center.domain;

import java.util.Map;

public record ComponentSchema(
        String name,
        Map<String, Object> schema
) {
}
