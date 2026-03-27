package com.example.swagger_center.dto;

public record RegisterSpecRequest(
        String serviceName,
        String openApiJson
) {
}
