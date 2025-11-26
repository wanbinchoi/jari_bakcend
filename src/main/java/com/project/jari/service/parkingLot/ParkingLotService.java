package com.project.jari.service.parkingLot;

import com.project.jari.client.KakaoMapApiClient;
import com.project.jari.client.ParkingApiClient;
import com.project.jari.dto.ParkingLotDto;
import com.project.jari.dto.parkingLot.ParkingLotInfo;
import com.project.jari.entity.parkingLot.ParkingLot;
import com.project.jari.exception.ParkingLotNotFoundException;
import com.project.jari.repository.parkingLot.ParkingLotRepository;
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

    // 죄표값 0으로 초기화
    private static final double DEFAULT_COORDINATE = 0.0;

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
                .nowVhclCnt(info.getNOW_PRK_VHCL_CNT() != null ? info.getNOW_PRK_VHCL_CNT().intValue() : 0)
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
     * 개별 주차장 처리
     * 
     * 처리 순서:
     * 1. 매핑 테이블에서 좌표 확인
     * 2. 매핑이 없으면 카카오 API 호출
     * 3. 모든 방법 실패 시 기본 좌표 사용
     * 
     * 면접 포인트:
     * - 다단계 fallback 전략을 구현하여 데이터 손실을 최소화
     * - 카카오 api로 변환되지 않는 주소는 개발자측에서 미리 파악 후 매핑테이블 작성
     * - 매핑 테이블을 우선 확인하여 API 호출 비용을 절약
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
                                    
                                    return saved;
                                })
                                .onErrorResume(apiError -> {
                                    // 카카오 API도 실패 -> 기본 좌표 사용
                                    ParkingLot parkingLot = convertToEntity(info, DEFAULT_COORDINATE, DEFAULT_COORDINATE);
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
                    
                    ParkingLot parkingLot = convertToEntity(info, DEFAULT_COORDINATE, DEFAULT_COORDINATE);
                    ParkingLot saved = parkingLotRepository.save(parkingLot);
                    failCount.incrementAndGet();
                    
                    return Mono.just(saved);
                });
    }


    /**
     * 병렬 주차장 데이터 동기화 (매핑 시스템 포함)
     * 
     * 면접 포인트:
     * - 매핑 테이블 활용으로 API 호출 비용을 줄이고 처리 속도를 향상
     * - 병렬 처리로 대용량 데이터를 효율적으로 처리
     * - 상세한 통계 정보로 시스템 성능을 모니터링 가능
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

    /**
    1. 이름, 주소 각각 검색하는 기능 구현했음
    2. 검색창에 구분해서 검색하는 기능을 만들까 했으나
       공통 검색하는 기능으로 리팩토링(추가하는 형식으로)
    3. ParkingLotRepository에 @Query로 직접 쿼리문으로 공통 검색 메소드 생성
    4. 구현
     @param keyword -> 검색할 키워드(String)
     **/

    /*
     * 통합 검색 (이름 + 주소)
     */
    public List<ParkingLotDto> searchByKeyword(String keyword) {

        // 1. 검색어 유효성 검사
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        // 2. DB에서 한 번에 조회 (이름 OR 주소)
        List<ParkingLot> parkingLots = parkingLotRepository.searchByKeyword(keyword);

        // 3. 결과 검증
        if (parkingLots.isEmpty()) {
            throw new ParkingLotNotFoundException(
                    "'" + keyword + "' 검색 결과가 존재하지 않습니다."
            );
        }

        // 4. DTO 변환 후 반환
        return parkingLots.stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
    }

    // 주차장 이름으로 검색
    @Transactional(readOnly = true)
    public List<ParkingLotDto> searchNameContaining(String name) {
        // 1. 검색어가 없으면 예외 발생
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        // 2. DB 조회
        List<ParkingLot> parkingLots = parkingLotRepository.findByNameContaining(name);

        // 3. 결과가 없으면 예외 발생
        if (parkingLots.isEmpty()) {
            throw new ParkingLotNotFoundException("'" + name + "' 검색 결과가 존재하지 않습니다.");
        }

        // 4. 결과가 있으면 DTO로 변환해서 반환
        return parkingLots.stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
    }

    // 주소로 검색
    @Transactional(readOnly = true)
    public List<ParkingLotDto> findByAddressContaining(String address) {
        // 1. 검색어가 없으면 예외 발생
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        // 2. DB 조회
        List<ParkingLot> parkingLots = parkingLotRepository.findByAddressContaining(address);

        // 3. 결과가 없으면 예외 발생
        if (parkingLots.isEmpty()) {
            throw new ParkingLotNotFoundException("'" + address + "' 검색 결과가 존재하지 않습니다.");
        }

        // 4. 결과가 있으면 DTO로 변환해서 반환
        return parkingLots.stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
    }
}
