package com.project.jari.client.impl;

import com.project.jari.client.KakaoMapApiClient;
import com.project.jari.config.KakaoMapProperties;
import com.project.jari.dto.response.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Kakao Map API 클라이언트 구현체
 * 주소를 위도/경도로 변환하는 기능 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoMapApiClientImpl implements KakaoMapApiClient {

    private final WebClient webClient;
    private final KakaoMapProperties kakaoMapProperties;

    /**
     * 주소를 위도/경도로 변환
     *
     * @param address 검색할 주소 (예: "종로구 세종로 80-1")
     * @return 좌표 배열 [위도, 경도] 또는 null
     */
    @Override
    public Double[] convertAddressToCoordinates(String address) {
        log.info("주소 변환 요청: {}", address);

        try {
            KakaoAddressResponse response = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoMapProperties.getApiKey())
                    .retrieve()
                    .bodyToMono(KakaoAddressResponse.class)
                    // 타임아웃 설정: 5초
                    .timeout(Duration.ofSeconds(5))
                    // 재시도 전략: 최대 2번, 1초 간격
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests))
                    .block(); // 동기식으로 변환 (Service 레이어 단순화)

            if (response == null) {
                log.warn("API 응답이 null: {}", address);
                return null;
            }

            Double[] coordinates = response.getFirstCoordinate();

            if (coordinates == null) {
                log.warn("좌표를 찾을 수 없음: {}", address);
                return null;
            }

            log.info("주소 변환 성공: {} -> 위도={}, 경도={}",
                    address, coordinates[0], coordinates[1]);

            return coordinates;

        } catch (WebClientResponseException e) {
            log.error("Kakao API 호출 실패 (HTTP {}): {} - {}",
                    e.getStatusCode(), address, e.getMessage());
            return null;

        } catch (Exception e) {
            log.error("주소 변환 중 오류 발생: {} - {}", address, e.getMessage(), e);
            return null;
        }
    }
}