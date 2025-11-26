package com.project.jari.service.parkingLot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.jari.dto.mapping.AddressMapping;
import com.project.jari.dto.mapping.AddressMappingFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 주소 매핑 관리 서비스
 * 
 * 면접 포인트:
 * - "변환 실패한 주소들을 효율적으로 관리하기 위해 Map 자료구조를 활용했습니다"
 * - "애플리케이션 시작 시 매핑 데이터를 메모리에 로드하여 빠른 조회가 가능합니다"
 * - "JSON 파일을 활용하여 코드 수정 없이 매핑 데이터를 업데이트할 수 있습니다"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressMappingService {
    
    private final ObjectMapper objectMapper;
    
    // 빠른 조회를 위한 Map (원본주소 -> 매핑정보)
    private final Map<String, AddressMapping> addressMappingCache = new ConcurrentHashMap<>();
    
    // 통계 정보
    private int totalMappings = 0;
    private String lastLoadTime;
    
    /**
     * 애플리케이션 시작 시 매핑 데이터 로드
     */
    @PostConstruct
    public void loadMappings() {
        try {
            log.info("=== 주소 매핑 데이터 로딩 시작 ===");
            
            ClassPathResource resource = new ClassPathResource("mapping/failed-address-mapping.json");
            
            if (!resource.exists()) {
                log.warn("매핑 파일을 찾을 수 없습니다: {}", resource.getPath());
                log.warn("현재 작업 디렉토리: {}", System.getProperty("user.dir"));
                return;
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                AddressMappingFile mappingFile = objectMapper.readValue(inputStream, AddressMappingFile.class);
                
                // 캐시에 데이터 저장 (키는 정규화된 원본주소)
                for (AddressMapping mapping : mappingFile.getMappings()) {
                    String normalizedKey = normalizeAddressForKey(mapping.getOriginalAddress());
                    addressMappingCache.put(normalizedKey, mapping);
                }
                
                totalMappings = mappingFile.getMappings().size();
                lastLoadTime = new Date().toString();
                
                log.info("=== 매핑 데이터 로딩 완료: {}건 ===", totalMappings);
                
                // 로딩된 매핑 정보 출력 (디버깅용)
                addressMappingCache.forEach((key, mapping) -> 
                    log.debug("매핑 로드: '{}' -> '{}'", 
                        mapping.getOriginalAddress(), mapping.getCorrectAddress())
                );
                
            }
            
        } catch (IOException e) {
            log.error("매핑 파일 로딩 중 오류 발생", e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
        }
    }
    
    /**
     * 대안 초기화 메소드 (@PostConstruct가 작동하지 않을 경우)
     */
    private void ensureInitialized() {
        if (addressMappingCache.isEmpty() && totalMappings == 0) {
            log.info("매핑 데이터가 로드되지 않은 상태, 수동 초기화 시도");
            loadMappings();
        }
    }
    
    /**
     * 주소를 매핑에서 찾아서 좌표 반환
     * 
     * @param originalAddress 원본 주소
     * @return 좌표 배열 [위도, 경도] 또는 null
     */
    public Optional<Double[]> findCoordinatesByAddress(String originalAddress) {
        ensureInitialized();  // 초기화 확인
        
        if (originalAddress == null || originalAddress.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalizedKey = normalizeAddressForKey(originalAddress);
        AddressMapping mapping = addressMappingCache.get(normalizedKey);
        
        if (mapping != null) {
            log.info("✅ 매핑에서 주소 발견: '{}' -> 좌표({}, {})", 
                originalAddress, mapping.getLatitude(), mapping.getLongitude());
            return Optional.of(mapping.getCoordinatesArray());
        }
        
        log.debug("❌ 매핑에서 주소를 찾을 수 없음: '{}'", originalAddress);
        return Optional.empty();
    }
    
    /**
     * 도로명 주소 반환
     */
    public Optional<String> findRoadNameAddress(String originalAddress) {
        ensureInitialized();  // 초기화 확인
        
        if (originalAddress == null || originalAddress.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalizedKey = normalizeAddressForKey(originalAddress);
        AddressMapping mapping = addressMappingCache.get(normalizedKey);
        
        if (mapping != null) {
            return Optional.of(mapping.getRoadNameAddress());
        }
        
        return Optional.empty();
    }
    
    /**
     * 모든 매핑 정보 반환 (관리용)
     */
    public List<AddressMapping> getAllMappings() {
        ensureInitialized();  // 초기화 확인
        return new ArrayList<>(addressMappingCache.values());
    }
    
    /**
     * 통계 정보 반환
     */
    public Map<String, Object> getMappingStatistics() {
        ensureInitialized();  // 초기화 확인
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMappings", totalMappings);
        stats.put("lastLoadTime", lastLoadTime);
        stats.put("cacheSize", addressMappingCache.size());
        
        // 검증 소스별 통계
        Map<String, Long> sourceStats = addressMappingCache.values().stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    AddressMapping::getVerificationSource,
                    java.util.stream.Collectors.counting()
                )
            );
        stats.put("sourceStatistics", sourceStats);
        
        return stats;
    }
    
    /**
     * 매핑 데이터 강제 새로고침
     */
    public void reloadMappings() {
        log.info("매핑 데이터 강제 새로고침 시작");
        addressMappingCache.clear();
        loadMappings();
    }
    
    /**
     * 주소를 키로 사용하기 위해 정규화
     * (대소문자, 공백 통일)
     */
    private String normalizeAddressForKey(String address) {
        if (address == null) {
            return "";
        }
        
        return address.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");  // 연속 공백을 하나로
    }
    
    /**
     * 새로운 매핑 추가 (런타임에 추가, 파일에는 저장하지 않음)
     * 
     * @param mapping 새로운 매핑 정보
     */
    public void addMapping(AddressMapping mapping) {
        String normalizedKey = normalizeAddressForKey(mapping.getOriginalAddress());
        addressMappingCache.put(normalizedKey, mapping);
        totalMappings = addressMappingCache.size();
        
        log.info("새로운 매핑 추가: '{}' -> '{}'", 
            mapping.getOriginalAddress(), mapping.getCorrectAddress());
    }
    
    /**
     * 특정 주소가 매핑에 존재하는지 확인
     */
    public boolean hasMappingFor(String address) {
        ensureInitialized();  // 초기화 확인
        String normalizedKey = normalizeAddressForKey(address);
        return addressMappingCache.containsKey(normalizedKey);
    }
}
