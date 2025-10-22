package com.project.jari.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 설정
 * Kakao Map API 호출을 위한 WebClient 빈 생성
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final KakaoMapProperties kakaoMapProperties;

    /**
     * Kakao API용 WebClient 빈
     *
     * baseUrl 설정으로 매번 전체 URL을 입력하지 않아도 됨
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(kakaoMapProperties.getBaseUrl())
                .build();
    }
}