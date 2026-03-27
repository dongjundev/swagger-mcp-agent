package com.example.ms_product.controller;

import com.example.ms_product.dto.CreateProductRequest;
import com.example.ms_product.dto.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    private final Map<Long, ProductDto> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @GetMapping
    @Operation(summary = "전체 상품 목록 조회", operationId = "listProducts")
    public List<ProductDto> listProducts() {
        return List.copyOf(store.values());
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 상세 조회", operationId = "getProduct")
    public ResponseEntity<ProductDto> getProduct(
            @Parameter(description = "상품 ID") @PathVariable Long id) {
        ProductDto product = store.get(id);
        return product != null ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    @Operation(summary = "카테고리별 상품 검색", operationId = "searchProducts")
    public List<ProductDto> searchProducts(
            @Parameter(description = "카테고리명") @RequestParam String category) {
        return store.values().stream()
                .filter(p -> p.category().equalsIgnoreCase(category))
                .toList();
    }

    @PostMapping
    @Operation(summary = "상품 등록", operationId = "createProduct")
    public ProductDto createProduct(@RequestBody CreateProductRequest request) {
        long id = sequence.getAndIncrement();
        ProductDto product = new ProductDto(id, request.name(), request.category(), request.price(), request.stock());
        store.put(id, product);
        return product;
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "재고 수량 변경", operationId = "updateStock")
    public ResponseEntity<ProductDto> updateStock(
            @Parameter(description = "상품 ID") @PathVariable Long id,
            @Parameter(description = "변경할 재고 수량") @RequestParam int quantity) {
        ProductDto existing = store.get(id);
        if (existing == null) return ResponseEntity.notFound().build();
        ProductDto updated = new ProductDto(existing.id(), existing.name(), existing.category(),
                existing.price(), quantity);
        store.put(id, updated);
        return ResponseEntity.ok(updated);
    }
}
