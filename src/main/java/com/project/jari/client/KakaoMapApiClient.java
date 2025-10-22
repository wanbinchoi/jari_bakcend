package com.project.jari.client;

import reactor.core.publisher.Mono;

import java.io.IOException;

public interface KakaoMapApiClient {
    // kakao api 호출
    Double[] convertAddressToCoordinates(String address) throws IOException;

    // 위도 경도 변환하는 메소드
    // 비동기로 배치처리하게 만들었음
    Mono<Double[]> convertAddressToCoordinatesAsync(String address);
}
