package com.project.jari.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.jari.client.ParkingApiClient;
import com.project.jari.dto.response.ParkingLotInfo;
import com.project.jari.dto.response.ParkingResponse;
import com.project.jari.entity.ParkingLot;
import com.project.jari.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ParkingLotService {

    private final ParkingApiClient parkingApiClient;
    private final ParkingLotRepository parkingLotRepository;
    private final ObjectMapper objectMapper;

    /**
     * 모든 주차장 조회 (DB에서)
     */
    public List<ParkingLot> findAll() {
        return parkingLotRepository.findAll();
    }

    /**
     * 주차장 코드로 조회
     */
    public ParkingLot findByCode(String pkltCode) {
        return parkingLotRepository.findById(pkltCode)
                .orElseThrow(() -> new IllegalArgumentException(
                    "주차장을 찾을 수 없습니다: " + pkltCode));
    }

    /**
     * 여러 주차장 코드로 조회
     */
    public List<ParkingLot> findByCodes(List<String> codes) {
        return parkingLotRepository.findByPkltCodeIn(codes);
    }

    /**
     * 이름으로 검색
     */
    public List<ParkingLot> searchByName(String keyword) {
        return parkingLotRepository.findByNameContaining(keyword);
    }

    /**
     * Open API에서 데이터 가져와서 DB에 저장
     */
    @Transactional
    public int syncParkingData() {
        log.info("=== 주차장 데이터 동기화 시작 ===");
        
        try {
            // 1. API 호출
            String rawData = parkingApiClient.callPkApi();
            ParkingResponse response = objectMapper.readValue(rawData, ParkingResponse.class);
            
            if (response == null || response.getGetParkingInfo() == null) {
                log.error("API 응답 없음");
                return 0;
            }
            
            List<ParkingLotInfo> apiData = response.getGetParkingInfo().getRow();
            
            if (apiData == null || apiData.isEmpty()) {
                log.warn("주차장 데이터 없음");
                return 0;
            }
            
            log.info("API 응답 수신: {}건", apiData.size());
            
            // 2. Entity로 변환
            List<ParkingLot> entities = new ArrayList<>();
            
            for (ParkingLotInfo info : apiData) {
                try {
                    ParkingLot entity = convertToEntity(info);
                    entities.add(entity);
                } catch (Exception e) {
                    log.warn("주차장 변환 실패: {}, 에러: {}", 
                        info.getPKLT_CD(), e.getMessage());
                }
            }
            
            // 3. DB에 저장
            parkingLotRepository.saveAll(entities);
            
            log.info("주차장 데이터 동기화 완료: {}건", entities.size());
            return entities.size();
            
        } catch (Exception e) {
            log.error("주차장 데이터 동기화 실패", e);
            throw new RuntimeException("데이터 동기화 실패", e);
        }
    }

    /**
     * API 응답 → Entity 변환 (단순화 버전)
     */
    private ParkingLot convertToEntity(ParkingLotInfo info) {
        // 운영시간 JSON 생성
        Map<String, Object> operationHours = createOperationHours(info);
        
        return ParkingLot.builder()
                .pkltCode(info.getPKLT_CD())
                .name(info.getPKLT_NM())
                .address(info.getADDR())
                .parkingType(info.getPRK_TYPE_NM())
                .operationType(info.getOPER_SE_NM())
                // TODO: Kakao API로 좌표 변환 필요
                .latitude(37.5665)  // 임시 기본값 (서울시청)
                .longitude(126.9780)
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
     * 운영시간 JSON 생성
     */
    private Map<String, Object> createOperationHours(ParkingLotInfo info) {
        Map<String, Object> hours = new HashMap<>();
        
        // 평일
        Map<String, String> weekday = new HashMap<>();
        weekday.put("open", info.getWD_OPER_BGNG_TM() != null ? info.getWD_OPER_BGNG_TM() : "00:00");
        weekday.put("close", info.getWD_OPER_END_TM() != null ? info.getWD_OPER_END_TM() : "24:00");
        hours.put("weekday", weekday);
        
        // 주말
        Map<String, String> weekend = new HashMap<>();
        weekend.put("open", info.getWE_OPER_BGNG_TM() != null ? info.getWE_OPER_BGNG_TM() : "00:00");
        weekend.put("close", info.getWE_OPER_END_TM() != null ? info.getWE_OPER_END_TM() : "24:00");
        hours.put("weekend", weekend);
        
        // 공휴일
        Map<String, String> holiday = new HashMap<>();
        holiday.put("open", info.getLHLDY_OPER_BGNG_TM() != null ? info.getLHLDY_OPER_BGNG_TM() : "00:00");
        holiday.put("close", info.getLHLDY_OPER_END_TM() != null ? info.getLHLDY_OPER_END_TM() : "24:00");
        hours.put("holiday", holiday);
        
        // 무료 여부
        hours.put("saturdayFree", "N".equals(info.getSAT_CHGD_FREE_SE()));
        hours.put("holidayFree", "N".equals(info.getLHLDY_CHGD_FREE_SE()));
        
        return hours;
    }

    /**
     * API에서 직접 데이터 조회 (DB 저장 없이)
     */
    public List<ParkingLotInfo> getAllParkingLotsFromApi() throws IOException {
        String rawData = parkingApiClient.callPkApi();
        ParkingResponse response = objectMapper.readValue(rawData, ParkingResponse.class);

        if (response.getGetParkingInfo() == null ||
                response.getGetParkingInfo().getRow() == null) {
            return Collections.emptyList();
        }

        return response.getGetParkingInfo().getRow();
    }
}
