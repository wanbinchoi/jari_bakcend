package com.project.jari.client.impl;

import com.project.jari.client.KakaoMapApiClient;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class KakaoMapApiClientImpl implements KakaoMapApiClient {

    // kakao map open api 사용해서 지도 가져오기 구현해야함
    @Override
    public String callMapApi() throws IOException {
        return "이거는 아직 안함";
    }
}
