package com.example.ms_order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 항목")
public record OrderItemDto(
        @Schema(description = "상품 ID", example = "101")
        Long productId,
        @Schema(description = "상품명", example = "무선 키보드")
        String productName,
        @Schema(description = "수량", example = "2")
        int quantity,
        @Schema(description = "단가", example = "15000")
        int unitPrice
) {
}
