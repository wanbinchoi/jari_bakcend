package com.project.jari.controller;

import com.project.jari.dto.ParkingLotDto;
import com.project.jari.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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


    // 4. 데이터 동기화 병렬처리
    @PostMapping("/sync-parallel")
    public Mono<ResponseEntity<Map<String, Object>>> syncParkingDataParallel() {
        log.info("병렬 동기화 API 호출");

        return parkingLotService.syncParkingDataParallel()
                .map(result -> {
                    log.info("병렬 동기화 API 응답 성공");
                    return ResponseEntity.ok(result);
                })
                .onErrorResume(error -> {
                    log.error("병렬 동기화 API 에러", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(Map.of(
                                    "status", "error",
                                    "message", error.getMessage()
                            )));
                });
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
