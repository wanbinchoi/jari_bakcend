package com.project.jari.controller;

import com.project.jari.dto.response.ParkingLotInfo;
import com.project.jari.service.ParkingLotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:3000") //리액트 엔드포인트
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    /**
     * Raw 데이터 반환 (개발/디버깅용)
     */
    @GetMapping("/parking/raw")
    public ResponseEntity<String> getRawParkingData() {
        try {
            String rawData = parkingLotService.getRawParkingData();
            return ResponseEntity.ok(rawData);
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body("API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * 노외주차장(PKLT_TYPE = "NW")만 반환
     */
    @GetMapping("/parking/outdoor")
    public ResponseEntity<?> getOutdoorParkingLots() {
        try {
            List<ParkingLotInfo> outdoorParkings = parkingLotService.getOutdoorParkingLots();

            Map<String, Object> response = new HashMap<>();
            response.put("total_count", outdoorParkings.size());
            response.put("parking_lots", outdoorParkings);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "데이터 처리 실패", "message", e.getMessage()));
        }
    }

    /**
     * 전체 주차장 데이터 (구조화된 형태)
     */
    @GetMapping("/parking")
    public ResponseEntity<?> getAllParkingLots() {
        try {
            List<ParkingLotInfo> allParkings = parkingLotService.getAllParkingLots();

            Map<String, Object> response = new HashMap<>();
            response.put("total_count", allParkings.size());
            response.put("parking_lots", allParkings);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "데이터 처리 실패", "message", e.getMessage()));
        }
    }
}