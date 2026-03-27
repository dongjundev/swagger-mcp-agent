package com.example.swagger_center.store;

import io.swagger.v3.oas.models.OpenAPI;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpecStore {

    record StoredSpec(String serviceName, String version, OpenAPI parsedSpec, Instant registeredAt) {
    }

    void save(StoredSpec spec);

    Optional<StoredSpec> findByServiceName(String serviceName);

    List<StoredSpec> findAll();

    boolean delete(String serviceName);
}
