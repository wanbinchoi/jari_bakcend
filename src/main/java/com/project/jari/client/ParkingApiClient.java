package com.project.jari.client;

import java.io.IOException;

public interface ParkingApiClient {
    // 주차장 api 호출
    String callPkApi() throws IOException;
}