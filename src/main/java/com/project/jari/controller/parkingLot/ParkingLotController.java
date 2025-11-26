package com.project.jari.controller.parkingLot;

import com.project.jari.dto.ParkingLotDto;
import com.project.jari.dto.api.ApiResponse;
import com.project.jari.service.parkingLot.ParkingLotService;
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
@RequestMapping("/parking-lots")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    /*
     * 모든 주차장 조회
     */
    @GetMapping
    public ResponseEntity<List<ParkingLotDto>> getAllParkingLots() {
        List<ParkingLotDto> parkingLots = parkingLotService.getAllParkingLots();
        log.info("조회 결과: {}건", parkingLots.size());
        return ResponseEntity.ok(parkingLots);
    }

    /*
     * 특정 주차장 상세 조회
     */
    @GetMapping("/{pkltCode}")
    public ResponseEntity<ParkingLotDto> getParkingLot(@PathVariable String pkltCode) {
        ParkingLotDto parkingLot = parkingLotService.getParkingLotByCode(pkltCode);
        return ResponseEntity.ok(parkingLot);
    }

    /**
     * 이름으로 검색
     * name가 없거나 비어있으면 전체 목록 반환
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<ApiResponse<List<ParkingLotDto>>> searchByName(
            @RequestParam(required = false, defaultValue = "") String name) {

        // 커스텀 예외처리로 controller 책임 분리 하였음
        List<ParkingLotDto> parkingLots = parkingLotService.searchNameContaining(name);

        log.info("검색 결과: {}건", parkingLots.size());
        return ResponseEntity.ok(ApiResponse.success(parkingLots.size()+"개의 주차장을 찾았습니다.",parkingLots));
    }

    /**
     * 주소로 검색
     * address가 없거나 비어있으면 전체 목록 반환
     */
    @GetMapping("/search-by-address")
    public ResponseEntity<ApiResponse<List<ParkingLotDto>>> searchByAddress(
            @RequestParam(required = false, defaultValue = "") String address) {

        // 커스텀 예외처리로 controller 책임 분리 하였음
        List<ParkingLotDto> parkingLots = parkingLotService.findByAddressContaining(address);

        log.info("검색 결과: {}건", parkingLots.size());
        return ResponseEntity.ok(ApiResponse.success(parkingLots.size()+"개의 주차장을 찾았습니다.",parkingLots));
    }

    /*
     * 통합 검색 (이름 + 주소)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ParkingLotDto>>> search(
            @RequestParam String keyword) {

        List<ParkingLotDto> result = parkingLotService.searchByKeyword(keyword);

        return ResponseEntity.ok(
                ApiResponse.success("주차장 "+result.size()+"개 발견이요~", result)
        );
    }

    // 데이터 동기화 병렬처리
    @PostMapping("/sync-parallel")
    public Mono<ResponseEntity<Map<String, Object>>> syncParkingDataParallel() {

        return parkingLotService.syncParkingDataParallel()
                .map(result -> ResponseEntity.ok(result)
                )
                .onErrorResume(error ->
                    Mono.just(ResponseEntity.internalServerError()
                            .body(Map.of(
                                    "status", "error",
                                    "message", error.getMessage()
                            )))
                );
    }

    /*
     * 헬스 체크
     * 애플리케이션 상태 및 DB 연결 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 만약 DB에서 데이터가 받아와 진다면 정상적으로 try문 실행
            List<ParkingLotDto> parkingLots = parkingLotService.getAllParkingLots();
            health.put("status", "UP");
            health.put("parkingLotCount", parkingLots.size());
            log.info("헬스 체크 성공: {}건", parkingLots.size());

        } catch (Exception e) {
            // DB에서 안받아와지면 catch
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            log.error("헬스 체크 실패", e);
        }

        return ResponseEntity.ok(health);
    }
}
