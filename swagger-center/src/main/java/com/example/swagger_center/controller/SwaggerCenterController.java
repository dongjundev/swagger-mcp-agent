package com.example.swagger_center.controller;

import com.example.swagger_center.domain.ApiDetail;
import com.example.swagger_center.domain.ComponentSchema;
import com.example.swagger_center.domain.ServiceInfo;
import com.example.swagger_center.dto.PagedResponse;
import com.example.swagger_center.dto.RegisterSpecRequest;
import com.example.swagger_center.domain.ApiSummary;
import com.example.swagger_center.service.SwaggerCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SwaggerCenterController {

    private final SwaggerCenterService service;

    @PostMapping("/specs")
    public ResponseEntity<ServiceInfo> registerSpec(@RequestBody RegisterSpecRequest request) {
        ServiceInfo info = service.registerSpec(request);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceInfo>> listServices() {
        return ResponseEntity.ok(service.listServices());
    }

    @GetMapping("/services/{serviceName}/apis")
    public ResponseEntity<PagedResponse<ApiSummary>> getApiList(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getApiList(serviceName, page, size));
    }

    @GetMapping("/services/{serviceName}/apis/{operationId}")
    public ResponseEntity<ApiDetail> getApiDetail(
            @PathVariable String serviceName,
            @PathVariable String operationId) {
        return ResponseEntity.ok(service.getApiDetail(serviceName, operationId));
    }

    @GetMapping("/services/{serviceName}/schemas/{schemaName}")
    public ResponseEntity<ComponentSchema> getComponentSchema(
            @PathVariable String serviceName,
            @PathVariable String schemaName) {
        return ResponseEntity.ok(service.getComponentSchema(serviceName, schemaName));
    }

    @DeleteMapping("/services/{serviceName}")
    public ResponseEntity<Void> deleteService(@PathVariable String serviceName) {
        if (service.deleteService(serviceName)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
