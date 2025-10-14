package com.project.jari.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 서울시 공영주차장 API 응답 DTO
 * API 문서: http://openapi.seoul.go.kr:8088/(인증키)/json/GetParkingInfo/1/5/
 */
@Data
public class GetParkingInfo {
    
    @JsonProperty("GetParkingInfo")
    private GetParkInfo getParkingInfo;
    
    @Data
    public static class GetParkInfo {
        @JsonProperty("list_total_count")
        private Integer listTotalCount;
        
        @JsonProperty("RESULT")
        private ApiResult result;
        
        @JsonProperty("row")
        private List<ParkingLotInfo> row;
    }
}
