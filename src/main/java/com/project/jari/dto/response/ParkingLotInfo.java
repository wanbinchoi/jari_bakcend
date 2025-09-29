package com.project.jari.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ParkingLotInfo {
    @JsonProperty("PKLT_CD")
    private String parkingLotCode;

    @JsonProperty("PKLT_NM")
    private String parkingLotName;

    @JsonProperty("ADDR")
    private String address;

    @JsonProperty("PKLT_TYPE")
    private String parkingLotType;

    @JsonProperty("PRK_TYPE_NM")
    private String parkingTypeName;

    @JsonProperty("TELNO")
    private String phoneNumber;

    @JsonProperty("TPKCT")
    private Double totalParkingCount;

    @JsonProperty("NOW_PRK_VHCL_CNT")
    private Double currentParkingCount;

    @JsonProperty("PAY_YN")
    private String payYn;

    @JsonProperty("PAY_YN_NM")
    private String payYnName;

    // 필요한 필드들 추가...
}