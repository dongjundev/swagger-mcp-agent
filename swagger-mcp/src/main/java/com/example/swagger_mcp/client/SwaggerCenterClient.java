package com.example.swagger_mcp.client;

import com.example.swagger_mcp.dto.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class SwaggerCenterClient {

    private final RestClient restClient;

    public SwaggerCenterClient(RestClient swaggerCenterRestClient) {
        this.restClient = swaggerCenterRestClient;
    }

    public List<ServiceInfo> listServices() {
        return restClient.get()
                .uri("/api/services")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public PagedResponse<ApiSummary> getApiList(String serviceName, String keyword, int page, int size) {
        return restClient.get()
                .uri("/api/services/{serviceName}/apis?keyword={keyword}&page={page}&size={size}",
                        serviceName, keyword == null ? "" : keyword, page, size)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ApiDetail getApiDetail(String serviceName, String operationId) {
        return restClient.get()
                .uri("/api/services/{serviceName}/apis/{operationId}",
                        serviceName, operationId)
                .retrieve()
                .body(ApiDetail.class);
    }

    public ComponentSchema getComponentSchema(String serviceName, String schemaName) {
        return restClient.get()
                .uri("/api/services/{serviceName}/schemas/{schemaName}",
                        serviceName, schemaName)
                .retrieve()
                .body(ComponentSchema.class);
    }
}
