package com.project.jari.service;

import com.project.jari.client.KakaoMapApiClient;
import com.project.jari.client.ParkingApiClient;
import com.project.jari.dto.ParkingLotDto;
import com.project.jari.dto.response.ParkingLotInfo;
import com.project.jari.entity.ParkingLot;
import com.project.jari.repository.ParkingLotRepository;
import com.project.jari.util.AddressCleanser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingApiClient parkingApiClient;
    private final KakaoMapApiClient kakaoMapApiClient;
    private final AddressCleanser addressCleanser;

    // 서울시청 좌표 (폴백용)
    private static final double DEFAULT_LATITUDE = 37.5665;
    private static final double DEFAULT_LONGITUDE = 126.9780;

    /**
     * ParkingLotInfo를 ParkingLot Entity로 변환
     *
     * @param info 서울시 API 응답 데이터
     * @param latitude Kakao API로 변환된 위도
     * @param longitude Kakao API로 변환된 경도
     * @return ParkingLot Entity
     */
    private ParkingLot convertToEntity(ParkingLotInfo info, Double latitude, Double longitude) {

        String rawAddress = info.getADDR();
        String cleansedAddress = addressCleanser.cleanseAddress(rawAddress);

        // 변경 로깅
        if (!rawAddress.equals(cleansedAddress)) {
            log.info("주소 정제됨: '{}' -> '{}'", rawAddress, cleansedAddress);
        }

        // 운영시간 JSON 생성
        Map<String, Object> operationHours = new HashMap<>();
        operationHours.put("weekday_start", info.getWD_OPER_BGNG_TM());
        operationHours.put("weekday_end", info.getWD_OPER_END_TM());
        operationHours.put("weekend_start", info.getWE_OPER_BGNG_TM());
        operationHours.put("weekend_end", info.getWE_OPER_END_TM());
        operationHours.put("holiday_start", info.getLHLDY_OPER_BGNG_TM());
        operationHours.put("holiday_end", info.getLHLDY_OPER_END_TM());

        return ParkingLot.builder()
                .pkltCode(info.getPKLT_CD())
                .name(info.getPKLT_NM())
                .address(cleansedAddress)  // 정제된 주소 사용
                .parkingType(info.getPKLT_TYPE())
                .operationType(info.getOPER_SE_NM())
                .latitude(latitude)
                .longitude(longitude)
                .totalCapacity(info.getTPKCT() != null ? info.getTPKCT().intValue() : 0)
                .isPaid("Y".equals(info.getPAY_YN()))
                .baseRate(info.getBSC_PRK_CRG() != null ? info.getBSC_PRK_CRG().intValue() : 0)
                .baseTime(info.getBSC_PRK_HR() != null ? info.getBSC_PRK_HR().intValue() : 0)
                .additionalRate(info.getADD_PRK_CRG() != null ? info.getADD_PRK_CRG().intValue() : 0)
                .additionalTime(info.getADD_PRK_HR() != null ? info.getADD_PRK_HR().intValue() : 0)
                .dayMaxRate(info.getDAY_MAX_CRG() != null ? info.getDAY_MAX_CRG().intValue() : 0)
                .operationHours(operationHours)
                .tel(info.getTELNO())
                .isShared("Y".equals(info.getSHRN_PKLT_YN()))
                .build();
    }

    /**
     * 개별 주차장 처리 (비동기) - 매핑 시스템 통합
     * 
     * 처리 순서:
     * 1. 매핑 테이블에서 좌표 확인
     * 2. 매핑이 없으면 카카오 API 호출
     * 3. 모든 방법 실패 시 기본 좌표 사용
     * 
     * 면접 포인트:
     * - "다단계 fallback 전략을 구현하여 데이터 손실을 최소화했습니다"
     * - "매핑 테이블을 우선 확인하여 API 호출 비용을 절약했습니다"
     */
    private Mono<ParkingLot> processParkingLotAsync(
            ParkingLotInfo info,
            AtomicInteger successCount,
            AtomicInteger failCount,
            AtomicInteger mappingSuccessCount) {

        String rawAddress = info.getADDR();
        String cleansedAddress = addressCleanser.cleanseAddress(rawAddress);

        // 1단계: 매핑 테이블에서 좌표 확인
        return Mono.fromCallable(() -> addressCleanser.findCoordinatesFromMapping(rawAddress))
                .flatMap(mappingResult -> {
                    if (mappingResult.isPresent()) {
                        // 매핑에서 좌표 발견
                        Double[] coordinates = mappingResult.get();
                        log.info("🗂️ 매핑 테이블 사용: {} -> ({}, {})", 
                            info.getPKLT_NM(), coordinates[0], coordinates[1]);
                        
                        ParkingLot parkingLot = convertToEntity(info, coordinates[0], coordinates[1]);
                        ParkingLot saved = parkingLotRepository.save(parkingLot);
                        mappingSuccessCount.incrementAndGet();
                        successCount.incrementAndGet();
                        
                        return Mono.just(saved);
                    } else {
                        // 매핑에 없음 -> 카카오 API 호출
                        return kakaoMapApiClient.convertAddressToCoordinatesAsync(cleansedAddress)
                                .delayElement(Duration.ofMillis(100))
                                .map(coordinates -> {
                                    ParkingLot parkingLot = convertToEntity(info, coordinates[0], coordinates[1]);
                                    ParkingLot saved = parkingLotRepository.save(parkingLot);
                                    successCount.incrementAndGet();
                                    
                                    log.debug("✅ 카카오 API 성공: {} (주소: {})",
                                            info.getPKLT_NM(), cleansedAddress);
                                    
                                    return saved;
                                })
                                .onErrorResume(apiError -> {
                                    // 카카오 API도 실패 -> 기본 좌표 사용
                                    log.warn("❌ 카카오 API 실패, 기본 좌표 사용: {} (주소: {})",
                                            info.getPKLT_NM(), cleansedAddress);
                                    
                                    ParkingLot parkingLot = convertToEntity(info, DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                                    ParkingLot saved = parkingLotRepository.save(parkingLot);
                                    failCount.incrementAndGet();
                                    
                                    return Mono.just(saved);
                                });
                    }
                })
                .onErrorResume(error -> {
                    // 전체 프로세스 실패 -> 기본 좌표 사용
                    log.error("❌ 전체 처리 실패: {} (주소: {})", 
                            info.getPKLT_NM(), rawAddress, error);
                    
                    ParkingLot parkingLot = convertToEntity(info, DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                    ParkingLot saved = parkingLotRepository.save(parkingLot);
                    failCount.incrementAndGet();
                    
                    return Mono.just(saved);
                });
    }


    /**
     * 병렬 주차장 데이터 동기화 (매핑 시스템 포함)
     * 
     * 면접 포인트:
     * - "매핑 테이블 활용으로 API 호출 비용을 줄이고 처리 속도를 향상했습니다"
     * - "병렬 처리로 대용량 데이터를 효율적으로 처리했습니다"
     * - "상세한 통계 정보로 시스템 성능을 모니터링할 수 있습니다"
     */
    @Transactional
    public Mono<Map<String, Object>> syncParkingDataParallel() {
        log.info("=== 매핑 시스템 포함 병렬 주차장 데이터 동기화 시작 ===");
        LocalDateTime startTime = LocalDateTime.now();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger mappingSuccessCount = new AtomicInteger(0);  // 매핑 성공 카운트

        return Mono.fromCallable(() -> parkingApiClient.getParkingInfo())
                .flatMapMany(parkingInfo -> {
                    List<ParkingLotInfo> parkingLots = parkingInfo.getGetParkingInfo().getRow();
                    log.info("처리할 주차장 수: {}", parkingLots.size());
                    return Flux.fromIterable(parkingLots);
                })
                .flatMap(info -> processParkingLotAsync(info, successCount, failCount, mappingSuccessCount), 10)
                .collectList()
                .map(results -> {
                    LocalDateTime endTime = LocalDateTime.now();
                    long processingTime = Duration.between(startTime, endTime).toSeconds();
                    
                    int apiCallCount = successCount.get() - mappingSuccessCount.get();
                    double mappingUsageRate = results.size() > 0 ? 
                        (mappingSuccessCount.get() * 100.0 / results.size()) : 0;

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "success");
                    resultMap.put("message", "주차장 데이터 동기화 완료 (매핑 시스템 포함)");
                    resultMap.put("totalCount", results.size());
                    resultMap.put("successCount", successCount.get());
                    resultMap.put("failCount", failCount.get());
                    resultMap.put("mappingSuccessCount", mappingSuccessCount.get());
                    resultMap.put("apiCallCount", apiCallCount);
                    resultMap.put("mappingUsageRate", String.format("%.1f%%", mappingUsageRate));
                    resultMap.put("processingTime", processingTime + "초");
                    resultMap.put("timestamp", endTime);

                    log.info("=== 처리 완료: {}초, 총: {}, 성공: {}, 실패: {}, 매핑 활용: {}건({}%) ===",
                            processingTime, results.size(), successCount.get(), failCount.get(),
                            mappingSuccessCount.get(), String.format("%.1f", mappingUsageRate));

                    return resultMap;
                })
                .doOnError(error -> log.error("동기화 중 에러 발생", error));
    }

    @Transactional(readOnly = true)
    public List<ParkingLotDto> getAllParkingLots() {
        return parkingLotRepository.findAll().stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ParkingLotDto getParkingLotByCode(String pkltCode) {
        ParkingLot parkingLot = parkingLotRepository.findById(pkltCode)
                .orElseThrow(() -> new RuntimeException("주차장을 찾을 수 없습니다: " + pkltCode));
        return ParkingLotDto.from(parkingLot);
    }

    // 이름으로 검색하는거
    @Transactional(readOnly = true)
    public List<ParkingLotDto> searchParkingLots(String keyword) {
        List<ParkingLot> parkingLots;

        if (keyword == null || keyword.trim().isEmpty()) {
            parkingLots = parkingLotRepository.findAll();
        } else {
            parkingLots = parkingLotRepository.findByNameContaining(keyword);
        }

        return parkingLots.stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
    }

    // 주소로 검색하는거
    @Transactional(readOnly = true)
    public List<ParkingLotDto> findByAddressContaining(String keyword) {
        List<ParkingLot> parkingLots;

        if (keyword == null || keyword.trim().isEmpty()) {
            parkingLots = parkingLotRepository.findAll();
        } else {
            parkingLots = parkingLotRepository.findByAddressContaining(keyword);
        }

        return parkingLots.stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
    }

    /*
    주소로도 검색하는 메소드를 만들었는데 service단에만 만들고
    controller에 기능 구현해야함
     */

}
