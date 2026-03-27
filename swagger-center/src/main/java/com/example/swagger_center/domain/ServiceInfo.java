package com.example.swagger_center.domain;

import java.time.Instant;

public record ServiceInfo(
        String serviceName,
        String version,
        int apiCount,
        Instant registeredAt
) {
}
