package com.example.swagger_center.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "swagger-center")
public record SwaggerCenterProperties(
        @DefaultValue Map<String, String> services,
        @DefaultValue("1m") Duration cacheTtl
) {
}
