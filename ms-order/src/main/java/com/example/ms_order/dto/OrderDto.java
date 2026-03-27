package com.example.ms_order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "주문 정보")
public record OrderDto(
        @Schema(description = "주문 ID", example = "1")
        Long id,
        @Schema(description = "주문자 ID", example = "42")
        Long userId,
        @Schema(description = "주문 항목 목록")
        List<OrderItemDto> items,
        @Schema(description = "총 금액", example = "35000")
        int totalAmount,
        @Schema(description = "주문 상태", example = "CONFIRMED")
        String status,
        @Schema(description = "주문 일시")
        Instant orderedAt
) {
}
