package com.example.swagger_center.parser;

import com.example.swagger_center.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OpenApiParser {

    public List<ApiSummary> extractApiSummaries(OpenAPI openAPI) {
        List<ApiSummary> summaries = new ArrayList<>();
        if (openAPI.getPaths() == null) return summaries;

        openAPI.getPaths().forEach((path, pathItem) -> {
            extractOperations(pathItem).forEach((method, operation) -> {
                String operationId = resolveOperationId(operation, method, path);
                summaries.add(new ApiSummary(
                        operationId,
                        method.toUpperCase(),
                        path,
                        operation.getSummary(),
                        operation.getTags(),
                        operation.getDeprecated()
                ));
            });
        });
        return summaries;
    }

    public ApiDetail extractApiDetail(OpenAPI openAPI, String operationId) {
        if (openAPI.getPaths() == null) return null;

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            for (Map.Entry<String, Operation> opEntry : extractOperations(pathItem).entrySet()) {
                String method = opEntry.getKey();
                Operation operation = opEntry.getValue();
                String resolvedId = resolveOperationId(operation, method, path);

                if (resolvedId.equals(operationId)) {
                    return buildApiDetail(openAPI, operation, resolvedId, method, path);
                }
            }
        }
        return null;
    }

    public ComponentSchema extractComponentSchema(OpenAPI openAPI, String schemaName) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return null;
        }
        Schema<?> schema = openAPI.getComponents().getSchemas().get(schemaName);
        if (schema == null) return null;

        return new ComponentSchema(schemaName, schemaToMap(mapperFor(openAPI), schema));
    }

    public List<String> listComponentSchemaNames(OpenAPI openAPI) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return List.of();
        }
        return new ArrayList<>(openAPI.getComponents().getSchemas().keySet());
    }

    public int countApis(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) return 0;
        int count = 0;
        for (PathItem pathItem : openAPI.getPaths().values()) {
            count += extractOperations(pathItem).size();
        }
        return count;
    }

    private ApiDetail buildApiDetail(OpenAPI openAPI, Operation operation,
                                     String operationId, String method, String path) {
        ObjectMapper mapper = mapperFor(openAPI);

        List<ParameterInfo> params = new ArrayList<>();
        if (operation.getParameters() != null) {
            for (Parameter p : operation.getParameters()) {
                params.add(new ParameterInfo(
                        p.getName(),
                        p.getIn(),
                        Boolean.TRUE.equals(p.getRequired()),
                        p.getDescription(),
                        schemaToMap(mapper, p.getSchema())
                ));
            }
        }

        Map<String, Object> requestBody = null;
        if (operation.getRequestBody() != null) {
            requestBody = mapper.convertValue(operation.getRequestBody(), Map.class);
        }

        Map<String, Object> responses = null;
        if (operation.getResponses() != null) {
            responses = mapper.convertValue(operation.getResponses(), Map.class);
        }

        List<SecurityRequirement> security = operation.getSecurity() != null
                ? operation.getSecurity()
                : openAPI.getSecurity();

        return new ApiDetail(
                operationId,
                method.toUpperCase(),
                path,
                operation.getSummary(),
                operation.getDescription(),
                params,
                requestBody,
                responses,
                security != null ? new ArrayList<>(security) : null,
                resolveSecuritySchemes(openAPI, mapper, security)
        );
    }

    private Map<String, Object> resolveSecuritySchemes(OpenAPI openAPI, ObjectMapper mapper,
                                                       List<SecurityRequirement> security) {
        if (security == null || openAPI.getComponents() == null
                || openAPI.getComponents().getSecuritySchemes() == null) {
            return null;
        }
        Map<String, Object> schemes = new LinkedHashMap<>();
        for (SecurityRequirement requirement : security) {
            for (String name : requirement.keySet()) {
                SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(name);
                if (scheme != null) {
                    schemes.put(name, mapper.convertValue(scheme, Map.class));
                }
            }
        }
        return schemes.isEmpty() ? null : schemes;
    }

    private Map<String, Operation> extractOperations(PathItem pathItem) {
        Map<String, Operation> operations = new LinkedHashMap<>();
        if (pathItem.getGet() != null) operations.put("GET", pathItem.getGet());
        if (pathItem.getPost() != null) operations.put("POST", pathItem.getPost());
        if (pathItem.getPut() != null) operations.put("PUT", pathItem.getPut());
        if (pathItem.getDelete() != null) operations.put("DELETE", pathItem.getDelete());
        if (pathItem.getPatch() != null) operations.put("PATCH", pathItem.getPatch());
        if (pathItem.getHead() != null) operations.put("HEAD", pathItem.getHead());
        if (pathItem.getOptions() != null) operations.put("OPTIONS", pathItem.getOptions());
        return operations;
    }

    private String resolveOperationId(Operation operation, String method, String path) {
        if (operation.getOperationId() != null && !operation.getOperationId().isBlank()) {
            return operation.getOperationId();
        }
        String sanitized = path.replaceAll("[{}/ ]", "_").replaceAll("^_|_$", "");
        return method.toLowerCase() + "_" + sanitized;
    }

    private ObjectMapper mapperFor(OpenAPI openAPI) {
        return openAPI.getSpecVersion() == SpecVersion.V31 ? Json31.mapper() : Json.mapper();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> schemaToMap(ObjectMapper mapper, Schema<?> schema) {
        if (schema == null) return null;
        return mapper.convertValue(schema, Map.class);
    }
}
