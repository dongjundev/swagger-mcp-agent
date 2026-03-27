package com.example.ms_user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보")
public record UserDto(
        @Schema(description = "사용자 ID", example = "1")
        Long id,
        @Schema(description = "사용자 이름", example = "홍길동")
        String name,
        @Schema(description = "이메일", example = "hong@example.com")
        String email,
        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone
) {
}
