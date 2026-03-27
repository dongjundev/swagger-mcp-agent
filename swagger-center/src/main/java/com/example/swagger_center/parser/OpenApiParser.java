package com.example.swagger_center.parser;

import com.example.swagger_center.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OpenApiParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                        operation.getSummary()
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
                    return buildApiDetail(operation, resolvedId, method, path);
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

        return new ComponentSchema(
                schemaName,
                schema.getType(),
                convertProperties(schema),
                schema.getRequired()
        );
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

    private ApiDetail buildApiDetail(Operation operation, String operationId, String method, String path) {
        List<ParameterInfo> params = new ArrayList<>();
        if (operation.getParameters() != null) {
            for (Parameter p : operation.getParameters()) {
                params.add(new ParameterInfo(
                        p.getName(),
                        p.getIn(),
                        Boolean.TRUE.equals(p.getRequired()),
                        p.getDescription(),
                        schemaToMap(p.getSchema())
                ));
            }
        }

        Map<String, Object> requestBody = null;
        if (operation.getRequestBody() != null) {
            requestBody = objectMapper.convertValue(operation.getRequestBody(), Map.class);
        }

        Map<String, Object> responses = null;
        if (operation.getResponses() != null) {
            responses = objectMapper.convertValue(operation.getResponses(), Map.class);
        }

        return new ApiDetail(
                operationId,
                method.toUpperCase(),
                path,
                operation.getSummary(),
                params,
                requestBody,
                responses
        );
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertProperties(Schema<?> schema) {
        if (schema.getProperties() == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        schema.getProperties().forEach((name, propSchema) -> {
            result.put((String) name, schemaToMap((Schema<?>) propSchema));
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> schemaToMap(Schema<?> schema) {
        if (schema == null) return null;
        return objectMapper.convertValue(schema, Map.class);
    }
}
