package com.example.swagger_mcp.tool;

import com.example.swagger_mcp.client.SwaggerCenterClient;
import com.example.swagger_mcp.dto.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SwaggerTools {

    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    private final SwaggerCenterClient client;

    public SwaggerTools(SwaggerCenterClient client) {
        this.client = client;
    }

    @Tool(description = "List all API services registered in the Swagger Center. "
            + "Returns service names, descriptions, server URLs, versions, and API counts. "
            + "Use this first to discover available services before querying specific APIs.")
    public List<ServiceInfo> listServices() {
        return client.listServices();
    }

    @Tool(description = "Get a lightweight list of APIs for a specific service. "
            + "Returns operationId, HTTP method, path, summary, and tags for each API. "
            + "Pass a keyword to narrow the list down instead of paging through a large service. "
            + "Use pagination for services with many APIs. "
            + "Call listServices first to find available service names.")
    public PagedResponse<ApiSummary> getApiList(
            @ToolParam(description = "The name of the service to query") String serviceName,
            @ToolParam(required = false, description = "Space-separated terms; only APIs whose operationId, "
                    + "path, summary, or tags contain every term are returned (case-insensitive)") String keyword,
            @ToolParam(description = "Page number (0-based), default 0") Integer page,
            @ToolParam(description = "Page size (1-100), default 20") Integer size) {
        int p = (page != null) ? page : 0;
        int s = (size != null) ? size : 20;
        return client.getApiList(serviceName, keyword, p, s);
    }

    @Tool(description = "Get full detail of a specific API operation including "
            + "description, parameters, request body schema, response schemas, and security requirements. "
            + "Use the operationId obtained from getApiList to query a specific API.")
    public ApiDetail getApiDetail(
            @ToolParam(description = "The name of the service") String serviceName,
            @ToolParam(description = "The operationId of the API to inspect") String operationId) {
        return client.getApiDetail(serviceName, operationId);
    }

    @Tool(description = "Resolve a component schema by name for a specific service. "
            + "Use this when an API detail contains $ref references to component schemas "
            + "that you need to inspect for full type details.")
    public ComponentSchema getComponentSchema(
            @ToolParam(description = "The name of the service") String serviceName,
            @ToolParam(description = "The schema name or its $ref path "
                    + "(e.g. 'Order' or '#/components/schemas/Order')") String schemaName) {
        String name = schemaName.startsWith(SCHEMA_REF_PREFIX)
                ? schemaName.substring(SCHEMA_REF_PREFIX.length())
                : schemaName;
        return client.getComponentSchema(serviceName, name);
    }
}
