package com.example.ms_product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 등록 요청")
public record CreateProductRequest(
        @Schema(description = "상품명", example = "무선 키보드", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "카테고리", example = "전자기기", requiredMode = Schema.RequiredMode.REQUIRED)
        String category,
        @Schema(description = "가격", example = "15000", requiredMode = Schema.RequiredMode.REQUIRED)
        int price,
        @Schema(description = "재고 수량", example = "50")
        int stock
) {
}
