package com.project.jari.dto.parkingLot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Open API 주차장 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotInfo {

    @JsonProperty("PKLT_CD")
    private String PKLT_CD;  // 주차장 코드

    @JsonProperty("PKLT_NM")
    private String PKLT_NM;  // 주차장 이름

    @JsonProperty("ADDR")
    private String ADDR;  // 주소

    @JsonProperty("PKLT_TYPE")
    private String PKLT_TYPE;  // 주차장 유형

    @JsonProperty("PRK_TYPE_NM")
    private String PRK_TYPE_NM;  // 주차장 종류명

    @JsonProperty("OPER_SE")
    private String OPER_SE;  // 운영 구분

    @JsonProperty("OPER_SE_NM")
    private String OPER_SE_NM;  // 운영 구분명

    @JsonProperty("TELNO")
    private String TELNO;  // 전화번호

    @JsonProperty("TPKCT")
    private Double TPKCT;  // 총 주차 대수

    @JsonProperty("NOW_PRK_VHCL_CNT")
    private Double NOW_PRK_VHCL_CNT;  // 현재 주차 차량 수

    @JsonProperty("NOW_PRK_VHCL_UPDT_TM")
    private String NOW_PRK_VHCL_UPDT_TM;  // 현재 주차 차량 업데이트 시간

    @JsonProperty("PAY_YN")
    private String PAY_YN;  // 유료 여부

    @JsonProperty("NGHT_PAY_YN")
    private String NGHT_PAY_YN;  // 야간 유료 여부

    @JsonProperty("WD_OPER_BGNG_TM")
    private String WD_OPER_BGNG_TM;  // 평일 운영 시작 시간

    @JsonProperty("WD_OPER_END_TM")
    private String WD_OPER_END_TM;  // 평일 운영 종료 시간

    @JsonProperty("WE_OPER_BGNG_TM")
    private String WE_OPER_BGNG_TM;  // 주말 운영 시작 시간

    @JsonProperty("WE_OPER_END_TM")
    private String WE_OPER_END_TM;  // 주말 운영 종료 시간

    @JsonProperty("LHLDY_OPER_BGNG_TM")
    private String LHLDY_OPER_BGNG_TM;  // 공휴일 운영 시작 시간

    @JsonProperty("LHLDY_OPER_END_TM")
    private String LHLDY_OPER_END_TM;  // 공휴일 운영 종료 시간

    @JsonProperty("SAT_CHGD_FREE_SE")
    private String SAT_CHGD_FREE_SE;  // 토요일 유무료 구분

    @JsonProperty("LHLDY_CHGD_FREE_SE")
    private String LHLDY_CHGD_FREE_SE;  // 공휴일 유무료 구분

    @JsonProperty("BSC_PRK_CRG")
    private Double BSC_PRK_CRG;  // 기본 주차 요금

    @JsonProperty("BSC_PRK_HR")
    private Double BSC_PRK_HR;  // 기본 주차 시간

    @JsonProperty("ADD_PRK_CRG")
    private Double ADD_PRK_CRG;  // 추가 주차 요금

    @JsonProperty("ADD_PRK_HR")
    private Double ADD_PRK_HR;  // 추가 주차 시간

    @JsonProperty("DAY_MAX_CRG")
    private Double DAY_MAX_CRG;  // 1일 최대 요금

    @JsonProperty("SHRN_PKLT_YN")
    private String SHRN_PKLT_YN;  // 공유 주차장 여부
}
