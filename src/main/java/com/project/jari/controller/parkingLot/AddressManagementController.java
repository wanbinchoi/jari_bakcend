package com.project.jari.controller.parkingLot;

import com.project.jari.dto.mapping.AddressMapping;
import com.project.jari.service.parkingLot.AddressMigrationService;
import com.project.jari.service.parkingLot.AddressMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 주소 데이터 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/address")
@RequiredArgsConstructor
public class AddressManagementController {

    private final AddressMigrationService migrationService;
    private final AddressMappingService addressMappingService;

    /**
     * 기존 DB 주소 일괄 정제
     * GET /api/admin/address/cleanse
     */
    @PostMapping("/cleanse")
    public ResponseEntity<Map<String, Object>> cleanseAddresses() {
        log.info("주소 정제 API 호출됨");

        int updatedCount = migrationService.cleanseExistingAddresses();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "주소 정제 완료");
        response.put("updatedCount", updatedCount);

        return ResponseEntity.ok(response);
    }

    /**
     * 끝에 "0"이 붙은 주소만 정제
     * POST /api/admin/address/cleanse/zero
     */
    @PostMapping("/cleanse/zero")
    public ResponseEntity<Map<String, Object>> cleanseZeroSuffix() {
        log.info("'0' 제거 API 호출됨");

        int updatedCount = migrationService.cleanseAddressesEndingWithZero();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "주소 끝 '0' 제거 완료");
        response.put("updatedCount", updatedCount);

        return ResponseEntity.ok(response);
    }
    
    /**
     * 매핑 데이터 통계 조회
     * GET /api/admin/address/mapping/stats
     */
    @GetMapping("/mapping/stats")
    public ResponseEntity<Map<String, Object>> getMappingStats() {
        log.info("매핑 통계 조회 API 호출됨");
        
        Map<String, Object> stats = addressMappingService.getMappingStatistics();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", stats);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 매핑 데이터 조회
     * GET /api/admin/address/mapping/all
     */
    @GetMapping("/mapping/all")
    public ResponseEntity<Map<String, Object>> getAllMappings() {
        log.info("전체 매핑 데이터 조회 API 호출됨");
        
        List<AddressMapping> mappings = addressMappingService.getAllMappings();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", mappings);
        response.put("count", mappings.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 주소로 매핑 조회
     * GET /api/admin/address/mapping/search?address={address}
     */
    @GetMapping("/mapping/search")
    public ResponseEntity<Map<String, Object>> searchMapping(@RequestParam String address) {
        log.info("주소 매핑 검색 API 호출됨: {}", address);
        
        Optional<Double[]> coordinates = addressMappingService.findCoordinatesByAddress(address);
        Optional<String> roadNameAddress = addressMappingService.findRoadNameAddress(address);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("originalAddress", address);
        response.put("found", coordinates.isPresent());
        
        if (coordinates.isPresent()) {
            response.put("coordinates", coordinates.get());
            response.put("roadNameAddress", roadNameAddress.orElse(null));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 매핑 데이터 강제 새로고침
     * POST /api/admin/address/mapping/reload
     */
    @PostMapping("/mapping/reload")
    public ResponseEntity<Map<String, Object>> reloadMappings() {
        log.info("매핑 데이터 새로고침 API 호출됨");
        
        addressMappingService.reloadMappings();
        Map<String, Object> stats = addressMappingService.getMappingStatistics();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "매핑 데이터 새로고침 완료");
        response.put("stats", stats);
        
        return ResponseEntity.ok(response);
    }
}
