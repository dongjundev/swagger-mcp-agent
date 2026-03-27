package com.example.swagger_center.service;

import com.example.swagger_center.domain.*;
import com.example.swagger_center.dto.PagedResponse;
import com.example.swagger_center.dto.RegisterSpecRequest;
import com.example.swagger_center.parser.OpenApiParser;
import com.example.swagger_center.store.SpecStore;
import com.example.swagger_center.store.SpecStore.StoredSpec;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwaggerCenterService {

    private final SpecStore specStore;
    private final OpenApiParser parser;

    public ServiceInfo registerSpec(RegisterSpecRequest request) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(request.openApiJson());
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            throw new IllegalArgumentException("Failed to parse OpenAPI spec: " + result.getMessages());
        }

        String version = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "unknown";
        Instant now = Instant.now();

        StoredSpec stored = new StoredSpec(request.serviceName(), version, openAPI, now);
        specStore.save(stored);

        return new ServiceInfo(
                request.serviceName(),
                version,
                parser.countApis(openAPI),
                now
        );
    }

    public List<ServiceInfo> listServices() {
        return specStore.findAll().stream()
                .map(stored -> new ServiceInfo(
                        stored.serviceName(),
                        stored.version(),
                        parser.countApis(stored.parsedSpec()),
                        stored.registeredAt()
                ))
                .toList();
    }

    public PagedResponse<ApiSummary> getApiList(String serviceName, int page, int size) {
        StoredSpec stored = specStore.findByServiceName(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        List<ApiSummary> all = parser.extractApiSummaries(stored.parsedSpec());
        return PagedResponse.of(all, page, size);
    }

    public ApiDetail getApiDetail(String serviceName, String operationId) {
        StoredSpec stored = specStore.findByServiceName(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        ApiDetail detail = parser.extractApiDetail(stored.parsedSpec(), operationId);
        if (detail == null) {
            throw new IllegalArgumentException("Operation not found: " + operationId);
        }
        return detail;
    }

    public ComponentSchema getComponentSchema(String serviceName, String schemaName) {
        StoredSpec stored = specStore.findByServiceName(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        ComponentSchema schema = parser.extractComponentSchema(stored.parsedSpec(), schemaName);
        if (schema == null) {
            throw new IllegalArgumentException("Schema not found: " + schemaName);
        }
        return schema;
    }

    public boolean deleteService(String serviceName) {
        return specStore.delete(serviceName);
    }
}
