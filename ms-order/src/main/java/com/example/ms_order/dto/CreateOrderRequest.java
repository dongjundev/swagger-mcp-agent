package com.example.ms_order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
        @Schema(description = "주문자 ID", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,
        @Schema(description = "주문 항목 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<OrderItemDto> items
) {
}
