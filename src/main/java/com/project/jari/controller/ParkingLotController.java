package com.project.jari.controller;

import com.project.jari.dto.ParkingLotDto;
import com.project.jari.dto.response.ParkingLotInfo;
import com.project.jari.entity.ParkingLot;
import com.project.jari.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parking-lots")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    /**
     * 1. 모든 주차장 조회 (DB에서)
     */
    @GetMapping
    public ResponseEntity<List<ParkingLotDto>> getAllParkingLots() {
        log.info("모든 주차장 조회 요청");
        List<ParkingLot> parkingLots = parkingLotService.findAll();
        
        List<ParkingLotDto> dtos = parkingLots.stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
        
        log.info("조회 결과: {}건", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 2. 특정 주차장 상세 조회
     */
    @GetMapping("/{pkltCode}")
    public ResponseEntity<ParkingLotDto> getParkingLot(@PathVariable String pkltCode) {
        log.info("주차장 상세 조회: {}", pkltCode);
        ParkingLot parkingLot = parkingLotService.findByCode(pkltCode);
        ParkingLotDto dto = ParkingLotDto.from(parkingLot);
        
        return ResponseEntity.ok(dto);
    }

    /**
     * 3. 이름으로 검색
     * keyword가 없거나 비어있으면 전체 목록 반환
     */
    @GetMapping("/search")
    public ResponseEntity<List<ParkingLotDto>> searchByName(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        
        log.info("=== 주차장 검색 요청 ===");
        log.info("원본 검색어: '{}'", keyword);

        List<ParkingLot> parkingLots;
        
        // 검색어가 비어있으면 전체 목록 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("검색어가 비어있음 -> 전체 목록 반환");
            parkingLots = parkingLotService.findAll();
        } else {
            // 공백 제거 후 검색
            String trimmedKeyword = keyword.trim();
            log.info("정제된 검색어: '{}'", trimmedKeyword);
            parkingLots = parkingLotService.searchByName(trimmedKeyword);
        }
        
        List<ParkingLotDto> dtos = parkingLots.stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
        
        log.info("검색 결과: {}건", dtos.size());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * 4. API 데이터 동기화 (관리자용)
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncParkingData() {
        log.info("주차장 데이터 동기화 요청");
        
        try {
            int count = parkingLotService.syncParkingData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "데이터 동기화 완료");
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("데이터 동기화 실패", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "데이터 동기화 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 5. API에서 직접 조회 (테스트용)
     */
    @GetMapping("/api-direct")
    public ResponseEntity<List<ParkingLotInfo>> getParkingLotsFromApi() {
        log.info("API에서 직접 조회");
        
        try {
            List<ParkingLotInfo> parkingLots = parkingLotService.getAllParkingLotsFromApi();
            return ResponseEntity.ok(parkingLots);
        } catch (IOException e) {
            log.error("API 조회 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 6. 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            long count = parkingLotService.findAll().size();
            health.put("status", "UP");
            health.put("parkingLotCount", count);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}
