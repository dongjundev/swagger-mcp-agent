package com.example.swagger_mcp.tool;

import com.example.swagger_mcp.client.SwaggerCenterClient;
import com.example.swagger_mcp.dto.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SwaggerTools {

    private final SwaggerCenterClient client;

    public SwaggerTools(SwaggerCenterClient client) {
        this.client = client;
    }

    @Tool(description = "List all API services registered in the Swagger Center. "
            + "Returns service names, versions, and API counts. "
            + "Use this first to discover available services before querying specific APIs.")
    public List<ServiceInfo> listServices() {
        return client.listServices();
    }

    @Tool(description = "Get a lightweight list of APIs for a specific service. "
            + "Returns operationId, HTTP method, path, and summary for each API. "
            + "Use pagination for services with many APIs. "
            + "Call listServices first to find available service names.")
    public PagedResponse<ApiSummary> getApiList(
            @ToolParam(description = "The name of the service to query") String serviceName,
            @ToolParam(description = "Page number (0-based), default 0") Integer page,
            @ToolParam(description = "Page size, default 20") Integer size) {
        int p = (page != null) ? page : 0;
        int s = (size != null) ? size : 20;
        return client.getApiList(serviceName, p, s);
    }

    @Tool(description = "Get full detail of a specific API operation including "
            + "parameters, request body schema, and response schemas. "
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
            @ToolParam(description = "The schema name to resolve (e.g. 'Pet', 'Order')") String schemaName) {
        return client.getComponentSchema(serviceName, schemaName);
    }
}
