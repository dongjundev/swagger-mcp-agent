package com.example.swagger_center.store;

import com.example.swagger_center.config.SwaggerCenterProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteSpecStore implements SpecStore {

    private final SwaggerCenterProperties properties;
    private final RestClient restClient;
    private final ConcurrentHashMap<String, StoredSpec> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<StoredSpec> findByServiceName(String serviceName) {
        String url = properties.services().get(serviceName);
        if (url == null) return Optional.empty();

        StoredSpec cached = cache.get(serviceName);
        if (cached != null && cached.fetchedAt().plus(properties.cacheTtl()).isAfter(Instant.now())) {
            return Optional.of(cached);
        }

        StoredSpec fetched = fetch(serviceName, url);
        cache.put(serviceName, fetched);
        return Optional.of(fetched);
    }

    @Override
    public List<StoredSpec> findAll() {
        List<StoredSpec> specs = new ArrayList<>();
        for (String serviceName : properties.services().keySet()) {
            try {
                findByServiceName(serviceName).ifPresent(specs::add);
            } catch (SpecFetchException e) {
                log.warn("Skipping service: {}", e.getMessage());
            }
        }
        return specs;
    }

    private StoredSpec fetch(String serviceName, String url) {
        String body;
        try {
            body = restClient.get().uri(url).retrieve().body(String.class);
        } catch (RestClientException e) {
            throw new SpecFetchException(
                    "Failed to fetch spec of " + serviceName + " from " + url + ": " + e.getMessage(), e);
        }

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(body);
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            throw new SpecFetchException(
                    "Failed to parse spec of " + serviceName + " from " + url + ": " + result.getMessages());
        }

        String version = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "unknown";
        return new StoredSpec(serviceName, version, openAPI, Instant.now());
    }
}
