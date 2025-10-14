package com.project.jari.controller;

import com.project.jari.dto.ParkingLotDto;
import com.project.jari.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주차장 정보 관리 Controller
 */
@RestController
@RequestMapping("/api/parking-lots")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    /**
     * 1. 모든 주차장 조회 (DB에서)
     * GET /api/parking-lots
     */
    @GetMapping
    public ResponseEntity<List<ParkingLotDto>> getAllParkingLots() {
        log.info("모든 주차장 조회 요청");
        List<ParkingLotDto> parkingLots = parkingLotService.getAllParkingLots();
        log.info("조회 결과: {}건", parkingLots.size());
        return ResponseEntity.ok(parkingLots);
    }

    /**
     * 2. 특정 주차장 상세 조회
     * GET /api/parking-lots/{pkltCode}
     */
    @GetMapping("/{pkltCode}")
    public ResponseEntity<ParkingLotDto> getParkingLot(@PathVariable String pkltCode) {
        log.info("주차장 상세 조회: {}", pkltCode);
        ParkingLotDto parkingLot = parkingLotService.getParkingLotByCode(pkltCode);
        return ResponseEntity.ok(parkingLot);
    }

    /**
     * 3. 이름으로 검색
     * GET /api/parking-lots/search?keyword=세종로
     * 
     * keyword가 없거나 비어있으면 전체 목록 반환
     */
    @GetMapping("/search")
    public ResponseEntity<List<ParkingLotDto>> searchByName(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        
        log.info("주차장 검색 요청: '{}'", keyword);
        
        // Service의 searchParkingLots()가 빈 키워드도 처리
        List<ParkingLotDto> parkingLots = parkingLotService.searchParkingLots(keyword);
        
        log.info("검색 결과: {}건", parkingLots.size());
        return ResponseEntity.ok(parkingLots);
    }

    /**
     * 4. API 데이터 동기화 (관리자용)
     * POST /api/parking-lots/sync
     * 
     * 서울시 Open API에서 데이터를 가져와 DB에 저장
     * Kakao Map API로 주소를 좌표로 변환
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
            
            log.info("데이터 동기화 성공: {}건", count);
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
     * 5. 헬스 체크
     * GET /api/parking-lots/health
     * 
     * 애플리케이션 상태 및 DB 연결 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            List<ParkingLotDto> parkingLots = parkingLotService.getAllParkingLots();
            health.put("status", "UP");
            health.put("parkingLotCount", parkingLots.size());
            log.info("헬스 체크 성공: {}건", parkingLots.size());
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            log.error("헬스 체크 실패", e);
        }
        
        return ResponseEntity.ok(health);
    }
}
