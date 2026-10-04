package com.example.swagger_center.service;

import com.example.swagger_center.domain.*;
import com.example.swagger_center.dto.PagedResponse;
import com.example.swagger_center.dto.RegisterSpecRequest;
import com.example.swagger_center.parser.OpenApiParser;
import com.example.swagger_center.store.SpecStore;
import com.example.swagger_center.store.SpecStore.StoredSpec;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SwaggerCenterService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SpecStore specStore;
    private final OpenApiParser parser;

    public ServiceInfo registerSpec(RegisterSpecRequest request) {
        if (request.serviceName() == null || request.serviceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(request.openApiJson());
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            throw new IllegalArgumentException("Failed to parse OpenAPI spec: " + result.getMessages());
        }

        String version = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "unknown";
        Instant now = Instant.now();

        StoredSpec stored = new StoredSpec(request.serviceName(), version, openAPI, now);
        specStore.save(stored);

        return toServiceInfo(stored);
    }

    public List<ServiceInfo> listServices() {
        return specStore.findAll().stream()
                .map(this::toServiceInfo)
                .toList();
    }

    public PagedResponse<ApiSummary> getApiList(String serviceName, String keyword, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        StoredSpec stored = specStore.findByServiceName(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        List<ApiSummary> all = parser.extractApiSummaries(stored.parsedSpec());
        if (keyword != null && !keyword.isBlank()) {
            String[] terms = keyword.trim().toLowerCase(Locale.ROOT).split("\\s+");
            all = all.stream().filter(api -> matches(api, terms)).toList();
        }
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

    private ServiceInfo toServiceInfo(StoredSpec stored) {
        OpenAPI openAPI = stored.parsedSpec();
        Info info = openAPI.getInfo();
        return new ServiceInfo(
                stored.serviceName(),
                info != null ? info.getTitle() : null,
                info != null ? info.getDescription() : null,
                stored.version(),
                openAPI.getServers() != null ? openAPI.getServers().stream().map(Server::getUrl).toList() : null,
                parser.countApis(openAPI),
                stored.registeredAt()
        );
    }

    private static boolean matches(ApiSummary api, String[] terms) {
        StringBuilder text = new StringBuilder(api.operationId()).append(' ').append(api.path());
        if (api.summary() != null) {
            text.append(' ').append(api.summary());
        }
        if (api.tags() != null) {
            text.append(' ').append(String.join(" ", api.tags()));
        }
        String haystack = text.toString().toLowerCase(Locale.ROOT);
        return Arrays.stream(terms).allMatch(haystack::contains);
    }
}
