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
     * @param info
     * @param successCount
     * @param failCount
     * @return 실제 DB에 저장되는 ParkingLot엔티티가 Mono로 싸여서 반환되는 것임.
     * 일단 개별 주차정 처리를 하는 메소드를 만들었음.
     * @implNote 이 메소드는 서울시 api에서 주차장 데이터를 받아옴, 받아온 데이터에서 주소를 카카오맵 api를 통해서 좌표로 변환 후
     * 완성된 ParkingLot엔티티를 DB에 저장시키는 역할
     */
    // 개별 주차장 처리 (비동기)
    private Mono<ParkingLot> processParkingLotAsync(
            ParkingLotInfo info,
            AtomicInteger successCount,
            AtomicInteger failCount) {

        // 주소 정제 후 지오코딩
        String rawAddress = info.getADDR();
        String cleansedAddress = addressCleanser.cleanseAddress(rawAddress);

        return kakaoMapApiClient.convertAddressToCoordinatesAsync(cleansedAddress)  // 정제된 주소 사용
                .delayElement(Duration.ofMillis(100))
                .map(coordinates -> {
                    ParkingLot parkingLot = convertToEntity(info, coordinates[0], coordinates[1]);
                    ParkingLot saved = parkingLotRepository.save(parkingLot);
                    successCount.incrementAndGet();

                    log.debug("✅ 주차장 저장 완료: {} (주소: {})",
                            info.getPKLT_NM(), cleansedAddress);

                    return saved;
                })
                .onErrorResume(error -> {
                    log.error("❌ 주차장 처리 실패: {} (주소: {})",
                            info.getPKLT_NM(), cleansedAddress, error);
                    ParkingLot parkingLot = convertToEntity(info, DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                    ParkingLot saved = parkingLotRepository.save(parkingLot);
                    failCount.incrementAndGet();
                    return Mono.just(saved);
                });
    }


    /**
     * @return 반환되는 값은 그냥 제대로 삽입되었나 확인할 수 있는 map반환
     * @implNote 위에 processParkingLotAsync()는 서울시 api에서 받아온 주차장을 하나씩 갖고와서
     * 카카오 api로 주소를 좌표로 변환해서 완성된 엔티티를 만드는 역할이었다면
     * syncParkingDataParallel() 이 메소드는 그거를 10개씩 묶어서 실행함 (병렬처리, Flux)
     * 따라서 실행시간이 확실히 줄어들음
     */
    @Transactional
    public Mono<Map<String, Object>> syncParkingDataParallel() {
        log.info("=== 병렬 주차장 데이터 동기화 시작 ===");
        LocalDateTime startTime = LocalDateTime.now();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        return Mono.fromCallable(() -> parkingApiClient.getParkingInfo())
                .flatMapMany(parkingInfo -> {
                    List<ParkingLotInfo> parkingLots = parkingInfo.getGetParkingInfo().getRow();
                    log.info("처리할 주차장 수: {}", parkingLots.size());
                    return Flux.fromIterable(parkingLots);
                })
                .flatMap(info -> processParkingLotAsync(info, successCount, failCount), 10)
                .collectList()
                .map(results -> {
                    LocalDateTime endTime = LocalDateTime.now();
                    long processingTime = Duration.between(startTime, endTime).toSeconds();

                    // 반환되는 값 확인해보기 위해서
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("status", "success");
                    resultMap.put("message", "주차장 데이터 동기화 완료 (병렬 처리)");
                    resultMap.put("totalCount", results.size());
                    resultMap.put("successCount", successCount.get());
                    resultMap.put("failCount", failCount.get());
                    resultMap.put("processingTime", processingTime + "초");
                    resultMap.put("timestamp", endTime);

                    log.info("=== 병렬 처리 완료: {}초, 성공: {}, 실패: {} ===",
                            processingTime, successCount.get(), failCount.get());

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
}