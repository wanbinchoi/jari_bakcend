package com.project.jari.client;

import java.io.IOException;

public interface KakaoMapApiClient {
    // kakao api 호출
    Double[] convertAddressToCoordinates(String address) throws IOException;
}
