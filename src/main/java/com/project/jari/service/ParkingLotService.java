package com.project.jari.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.jari.client.ParkingApiClient;
import com.project.jari.dto.response.ParkingLotInfo;
import com.project.jari.dto.response.ParkingResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingLotService {

    private final ParkingApiClient parkingApiClient;
    private final ObjectMapper objectMapper;

    public ParkingLotService(ParkingApiClient parkingApiClient, ObjectMapper objectMapper) {
        this.parkingApiClient = parkingApiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Raw 데이터 반환 (정제하지 않은 원본 데이터)
     */
    public String getRawParkingData() throws IOException {
        return parkingApiClient.callPkApi();
    }

    /**
     * PKLT_TYPE이 "NW"인 노외주차장 데이터만 필터링해서 반환
     */
    public List<ParkingLotInfo> getOutdoorParkingLots() throws IOException {
        // 1. Raw 데이터 가져오기
        String rawData = parkingApiClient.callPkApi();

        // 2. JSON 파싱
        ParkingResponse response = objectMapper.readValue(rawData, ParkingResponse.class);

        // 3. 데이터 검증
        if (response.getGetParkingInfo() == null ||
                response.getGetParkingInfo().getRow() == null) {
            return Collections.emptyList();
        }

        // 4. PKLT_TYPE이 "NW"인 데이터만 필터링
        return response.getGetParkingInfo().getRow()
                .stream()
                .filter(parking -> "NW".equals(parking.getParkingLotType()))
                .collect(Collectors.toList());
    }

    /**
     * api 응답 데이터 중에서 주차장 데이터만 갖고오기
     */
    public List<ParkingLotInfo> getAllParkingLots() throws IOException {
        String rawData = parkingApiClient.callPkApi();
        ParkingResponse response = objectMapper.readValue(rawData, ParkingResponse.class);

        if (response.getGetParkingInfo() == null ||
                response.getGetParkingInfo().getRow() == null) {
            return Collections.emptyList();
        }

        return response.getGetParkingInfo().getRow();
    }
}