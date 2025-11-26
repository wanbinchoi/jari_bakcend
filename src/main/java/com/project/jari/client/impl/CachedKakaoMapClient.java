package com.project.jari.client.impl;

import com.project.jari.config.KakaoMapProperties;
import com.project.jari.dto.parkingLot.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Kakao Map API 호출 + 캐싱 통합 레이어
 *
 * 왜 이렇게 설계했나요?
 * - WebClient를 직접 주입받아 순환 참조 방지
 * - 캐싱과 API 호출을 한 곳에서 관리
 * - Spring AOP Proxy가 정상 작동
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CachedKakaoMapClient {

    private final WebClient webClient;
    private final KakaoMapProperties kakaoMapProperties;

    private static final Double DEFAULT_LAT = 37.5665;
    private static final Double DEFAULT_LNG = 126.9780;

    /**
     * 주소를 좌표로 변환 (캐싱 적용)
     *
     * @param address 변환할 주소
     * @return [위도, 경도] 배열
     */
    @Cacheable(
            value = "coordinates",  // 캐시명
            key = "#address",  // 캐시 키값
            unless = "#result == null || #result.length == 0"  // null값 지정
    )
    public Double[] getCoordinatesWithCache(String address) {
        log.info("캐시 미스 - API 호출: {}", address);

        // 실제 Kakao API 호출
        // 해서 캐시에 값 저장
        return callKakaoApi(address);
    }

    /**
     * Kakao Map API 호출 (실제 구현)
     */
    private Double[] callKakaoApi(String address) {
        String apiKey = kakaoMapProperties.getApiKey();

        try {
            KakaoAddressResponse response = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .bodyToMono(KakaoAddressResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1))
                            .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                    .block();

            if (response != null && response.getFirstCoordinate() != null) {
                Double[] coords = response.getFirstCoordinate();
                log.info("✅ 좌표 변환 성공: {} -> [{}, {}]",
                        address, coords[0], coords[1]);
                return coords;
            }

            log.warn("⚠️ 좌표 변환 실패 (응답 없음): {}", address);
            return new Double[]{DEFAULT_LAT, DEFAULT_LNG};

        } catch (Exception e) {
            log.error("API 호출 중 에러 발생: {}", address, e);
            return new Double[]{DEFAULT_LAT, DEFAULT_LNG};
        }
    }
}