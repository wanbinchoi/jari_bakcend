package com.project.jari.service;

import com.project.jari.client.KakaoMapApiClient;
import com.project.jari.client.ParkingApiClient;
import com.project.jari.dto.ParkingLotDto;
import com.project.jari.dto.response.GetParkingInfo;
import com.project.jari.dto.response.ParkingLotInfo;
import com.project.jari.entity.ParkingLot;
import com.project.jari.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingApiClient parkingApiClient;
    private final KakaoMapApiClient kakaoMapApiClient;

    // 서울시청 좌표 (폴백용)
    private static final double DEFAULT_LATITUDE = 37.5665;
    private static final double DEFAULT_LONGITUDE = 126.9780;

    /**
     * 서울시 공영주차장 데이터 동기화
     * Kakao Map API를 통해 주소를 좌표로 변환하여 저장
     */
    @Transactional
    public int syncParkingData() {
        log.info("주차장 데이터 동기화 시작");

        int totalCount = 0;
        int successCount = 0;
        int coordinateSuccessCount = 0;
        int coordinateFailCount = 0;

        try {
            // 1. 서울시 API에서 주차장 데이터 가져오기
            GetParkingInfo response = parkingApiClient.getParkingInfo();
            List<ParkingLotInfo> parkingLots = response.getGetParkingInfo().getRow();
            totalCount = parkingLots.size();

            log.info("가져온 주차장 데이터: {}건", totalCount);

            // 2. 각 주차장 데이터를 Entity로 변환하여 저장
            for (ParkingLotInfo info : parkingLots) {
                try {
                    // 주소를 좌표로 변환
                    String address = info.getADDR();
                    Double[] coordinates = kakaoMapApiClient.convertAddressToCoordinates(address);

                    ParkingLot parkingLot;

                    if (coordinates != null && coordinates[0] != null && coordinates[1] != null) {
                        // 좌표 변환 성공
                        parkingLot = convertToEntity(info, coordinates[0], coordinates[1]);
                        coordinateSuccessCount++;
                        log.debug("좌표 변환 성공: {} -> 위도={}, 경도={}", 
                            address, coordinates[0], coordinates[1]);
                    } else {
                        // 좌표 변환 실패 -> 기본 좌표 사용
                        parkingLot = convertToEntity(info, DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                        coordinateFailCount++;
                        log.warn("좌표 변환 실패, 기본 좌표 사용: {}", address);
                    }

                    parkingLotRepository.save(parkingLot);
                    successCount++;

                    // API 호출 제한 고려: 약간의 지연 추가
                    Thread.sleep(100); // 0.1초 대기

                } catch (Exception e) {
                    log.error("주차장 데이터 저장 실패: {}", info.getPKLT_NM(), e);
                }
            }

            log.info("주차장 데이터 동기화 완료: 전체 {}건 중 {}건 성공 (좌표 변환: 성공 {}건, 실패 {}건)",
                    totalCount, successCount, coordinateSuccessCount, coordinateFailCount);

        } catch (Exception e) {
            log.error("주차장 데이터 동기화 중 오류 발생", e);
            throw new RuntimeException("주차장 데이터 동기화 실패", e);
        }

        // 결과 반환
//        Map<String, Object> result = new HashMap<>();
//        result.put("totalCount", totalCount);
//        result.put("successCount", successCount);
//        result.put("coordinateSuccessCount", coordinateSuccessCount);
//        result.put("coordinateFailCount", coordinateFailCount);
//        result.put("successRate", totalCount > 0 ?
//            String.format("%.2f%%", (double) successCount / totalCount * 100) : "0.00%");
//        result.put("coordinateSuccessRate", totalCount > 0 ?
//            String.format("%.2f%%", (double) coordinateSuccessCount / totalCount * 100) : "0.00%");
//
//        return result;
        return 1;
    }

    /**
     * ParkingLotInfo를 ParkingLot Entity로 변환
     * 
     * @param info 서울시 API 응답 데이터
     * @param latitude Kakao API로 변환된 위도
     * @param longitude Kakao API로 변환된 경도
     * @return ParkingLot Entity
     */
    private ParkingLot convertToEntity(ParkingLotInfo info, Double latitude, Double longitude) {
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
                .address(info.getADDR())
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
     * 전체 주차장 조회
     */
    @Transactional(readOnly = true)
    public List<ParkingLotDto> getAllParkingLots() {
        return parkingLotRepository.findAll().stream()
                .map(ParkingLotDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 주차장 코드로 조회
     */
    @Transactional(readOnly = true)
    public ParkingLotDto getParkingLotByCode(String pkltCode) {
        ParkingLot parkingLot = parkingLotRepository.findById(pkltCode)
                .orElseThrow(() -> new RuntimeException("주차장을 찾을 수 없습니다: " + pkltCode));
        return ParkingLotDto.from(parkingLot);
    }

    /**
     * 주차장 이름으로 검색
     */
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
