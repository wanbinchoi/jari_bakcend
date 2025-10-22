package com.project.jari.client.impl;

import com.project.jari.client.KakaoMapApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * KakaoMapApiClient 구현체
 * CachedKakaoMapClient에 위임하는 방식으로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoMapApiClientImpl implements KakaoMapApiClient {

    private final CachedKakaoMapClient cachedKakaoMapClient;

    private static final Double DEFAULT_LAT = 37.5665;
    private static final Double DEFAULT_LNG = 126.9780;

    /**
     * 동기 방식 주소 변환 (캐싱 적용됨)
     */
    @Override
    public Double[] convertAddressToCoordinates(String address) {
        return cachedKakaoMapClient.getCoordinatesWithCache(address);
    }

    /**
     * 비동기 방식 주소 변환 (캐싱 적용됨)
     */
    @Override
    public Mono<Double[]> convertAddressToCoordinatesAsync(String address) {
        return Mono.fromCallable(() ->
                        cachedKakaoMapClient.getCoordinatesWithCache(address)
                )
                .onErrorResume(error -> {
                    log.error("주소 변환 실패: {}", address, error);
                    return Mono.just(new Double[]{DEFAULT_LAT, DEFAULT_LNG});
                });
    }
}