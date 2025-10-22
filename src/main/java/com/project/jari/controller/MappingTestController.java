package com.project.jari.controller;

import com.project.jari.dto.mapping.AddressMapping;
import com.project.jari.service.AddressMappingService;
import com.project.jari.util.AddressCleanser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 매핑 시스템 테스트용 컨트롤러
 * 
 * 테스트 API:
 * - GET /api/test/mapping?address={주소} : 특정 주소의 매핑 결과 확인
 * - GET /api/test/cleanse?address={주소} : 주소 정제 결과 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class MappingTestController {
    
    private final AddressMappingService addressMappingService;
    private final AddressCleanser addressCleanser;
    
    /**
     * 주소 매핑 테스트
     * GET /api/test/mapping?address=서울특별시 강남구 도곡동 467-12 0
     */
    @GetMapping("/mapping")
    public ResponseEntity<Map<String, Object>> testMapping(@RequestParam String address) {
        log.info("매핑 테스트 API 호출: {}", address);
        
        Map<String, Object> result = new HashMap<>();
        result.put("originalAddress", address);
        
        // 1. 매핑 테이블에서 좌표 찾기
        Optional<Double[]> coordinates = addressMappingService.findCoordinatesByAddress(address);
        result.put("mappingFound", coordinates.isPresent());
        
        if (coordinates.isPresent()) {
            result.put("coordinates", coordinates.get());
            
            // 도로명주소도 찾기
            Optional<String> roadName = addressMappingService.findRoadNameAddress(address);
            result.put("roadNameAddress", roadName.orElse(null));
        }
        
        // 2. 주소 정제 결과
        String cleansedAddress = addressCleanser.cleanseAddress(address);
        result.put("cleansedAddress", cleansedAddress);
        
        // 3. 매핑 존재 여부
        boolean hasMapping = addressMappingService.hasMappingFor(address);
        result.put("hasMapping", hasMapping);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 주소 정제 테스트
     * GET /api/test/cleanse?address=서울특별시 강남구 테헤란로 152 0
     */
    @GetMapping("/cleanse")
    public ResponseEntity<Map<String, Object>> testCleanse(@RequestParam String address) {
        log.info("주소 정제 테스트 API 호출: {}", address);
        
        Map<String, Object> result = new HashMap<>();
        result.put("originalAddress", address);
        
        // 기본 정제
        String cleansedAddress = addressCleanser.cleanseAddress(address);
        result.put("cleansedAddress", cleansedAddress);
        
        // 변경 여부
        boolean changed = !address.equals(cleansedAddress);
        result.put("changed", changed);
        
        // 매핑에서 좌표 찾기 (정제 전 주소로)
        Optional<Double[]> mappingCoords = addressCleanser.findCoordinatesFromMapping(address);
        result.put("mappingCoordinates", mappingCoords.orElse(null));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 기본 초기화 상태 확인
     * GET /api/test/init
     */
    @GetMapping("/init")
    public ResponseEntity<Map<String, Object>> checkInitialization() {
        log.info("초기화 상태 확인 API 호출");
        
        Map<String, Object> result = new HashMap<>();
        
        // 매핑 서비스 상태 확인
        Map<String, Object> stats = addressMappingService.getMappingStatistics();
        result.put("mappingServiceStats", stats);
        
        // 직접 매핑 테이블 크기 확인
        List<AddressMapping> allMappings = addressMappingService.getAllMappings();
        result.put("totalMappingsInCache", allMappings.size());
        
        // 초기화 성공 여부
        boolean initialized = !allMappings.isEmpty();
        result.put("initialized", initialized);
        
        if (initialized) {
            result.put("message", "매핑 시스템 정상 초기화 완료");
            result.put("sampleMappings", allMappings.stream().limit(3).toList());
        } else {
            result.put("message", "매핑 데이터가 로드되지 않았습니다. @PostConstruct 문제일 수 있습니다.");
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 수동 매핑 데이터 로드 테스트
     * POST /api/test/manual-load
     */
    @PostMapping("/manual-load")
    public ResponseEntity<Map<String, Object>> manualLoadTest() {
        log.info("수동 매핑 데이터 로드 테스트 API 호출");
        
        try {
            // 강제 새로고침
            addressMappingService.reloadMappings();
            
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = addressMappingService.getMappingStatistics();
            
            result.put("status", "success");
            result.put("message", "수동 로드 성공");
            result.put("stats", stats);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("수동 로드 실패", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "수동 로드 실패: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
}
