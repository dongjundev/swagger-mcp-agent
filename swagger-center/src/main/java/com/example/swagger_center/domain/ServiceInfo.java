package com.example.swagger_center.domain;

import java.time.Instant;
import java.util.List;

public record ServiceInfo(
        String serviceName,
        String title,
        String description,
        String version,
        List<String> servers,
        int apiCount,
        Instant fetchedAt
) {
}
