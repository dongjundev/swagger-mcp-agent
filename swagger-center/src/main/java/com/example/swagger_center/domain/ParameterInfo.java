package com.example.swagger_center.domain;

import java.util.Map;

public record ParameterInfo(
        String name,
        String in,
        boolean required,
        String description,
        Map<String, Object> schema
) {
}
