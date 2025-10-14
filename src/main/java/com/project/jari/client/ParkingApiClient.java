package com.project.jari.client;

import com.project.jari.dto.response.GetParkingInfo;

import java.io.IOException;

/**
 * 서울시 공영주차장 API 클라이언트
 */
public interface ParkingApiClient {
    
    /**
     * 주차장 API 호출 (Raw JSON 문자열 반환)
     * @return JSON 문자열
     * @throws IOException API 호출 실패 시
     */
    String callPkApi() throws IOException;
    
    /**
     * 주차장 정보 조회 (파싱된 DTO 반환)
     * @return 주차장 정보 DTO
     * @throws IOException API 호출 또는 파싱 실패 시
     */
    GetParkingInfo getParkingInfo() throws IOException;
}
