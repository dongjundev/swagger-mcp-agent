package com.example.ms_product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 정보")
public record ProductDto(
        @Schema(description = "상품 ID", example = "101")
        Long id,
        @Schema(description = "상품명", example = "무선 키보드")
        String name,
        @Schema(description = "카테고리", example = "전자기기")
        String category,
        @Schema(description = "가격", example = "15000")
        int price,
        @Schema(description = "재고 수량", example = "50")
        int stock
) {
}
