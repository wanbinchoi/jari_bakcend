package com.project.jari.dto.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String accessToken;      // 액세스 토큰 (짧은 유효기간)
    private String refreshToken;     // 리프레시 토큰 (긴 유효기간)
    private String tokenType;        // 토큰 타입 (보통 "Bearer")
    private Long expiresIn;          // 액세스 토큰 만료까지 남은 시간 (초)

    private String mbId;
    private String mbNm;
}
