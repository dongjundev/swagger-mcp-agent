package com.example.ms_user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 생성 요청")
public record CreateUserRequest(
        @Schema(description = "사용자 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "이메일", example = "hong@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone
) {
}
