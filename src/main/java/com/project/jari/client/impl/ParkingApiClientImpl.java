package com.project.jari.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.jari.client.ParkingApiClient;
import com.project.jari.config.SeoulApiProperties;
import com.project.jari.dto.response.GetParkingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * 서울시 공영주차장 API 클라이언트 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingApiClientImpl implements ParkingApiClient {

    private final SeoulApiProperties props;
    private final ObjectMapper objectMapper;

    /**
     * Open API로 주차장 데이터 호출 (Raw JSON 반환)
     * 
     * @return JSON 문자열
     * @throws IOException API 호출 실패 시
     */
    @Override
    public String callPkApi() throws IOException {
        log.info("서울시 공영주차장 API 호출 시작");
        
        // URL 생성
        StringBuilder urlBuilder = new StringBuilder(props.getBaseUrl());
        urlBuilder.append("/" + URLEncoder.encode(props.getKey(), "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode(props.getResponseType(), "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode(props.getServiceName(), "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("178", "UTF-8"));

        /*
         * 선택사항: 구 단위 검색
         * urlBuilder.append("/" + URLEncoder.encode("마포구", "UTF-8"));
         * 지도 왼쪽 검색창에서 구 검색 기능 추가 가능
         */

        URL url = new URL(urlBuilder.toString());
        log.debug("API URL: {}", url.toString());
        
        // HTTP 연결
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        
        int responseCode = conn.getResponseCode();
        log.info("API 응답 코드: {}", responseCode);

        // 응답 읽기
        BufferedReader rd;
        if (responseCode >= 200 && responseCode <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        String jsonResponse = sb.toString();
        log.debug("API 응답 길이: {} bytes", jsonResponse.length());
        
        return jsonResponse;
    }

    // 위에 callPkApi()로 가져온 데이터를 DTO로 파싱하는 메소드
    /**
     * 주차장 정보 조회 (파싱된 DTO 반환)
     * 
     * @return 주차장 정보 DTO
     * @throws IOException API 호출 또는 파싱 실패 시
     */
    @Override
    public GetParkingInfo getParkingInfo() throws IOException {
        log.info("주차장 정보 조회 시작");
        
        try {
            // 1. API 호출하여 JSON 문자열 받기
            String jsonResponse = callPkApi();

            // 2. JSON 문자열을 DTO로 파싱
            GetParkingInfo parkingInfo = objectMapper.readValue(jsonResponse, GetParkingInfo.class);

            // 3. 파싱 결과 검증
            if (parkingInfo == null || parkingInfo.getGetParkingInfo() == null) {
                log.error("API 응답 파싱 실패: parkingInfo가 null");
                throw new IOException("API 응답 데이터가 올바르지 않습니다");
            }
            
            int count = parkingInfo.getGetParkingInfo().getRow() != null ? parkingInfo.getGetParkingInfo().getRow().size() : 0;
            
            log.info("주차장 정보 조회 완료: {}건", count);
            
            return parkingInfo;
            
        } catch (IOException e) {
            log.error("주차장 정보 조회 중 오류 발생", e);
            throw e;
        }
    }
}
