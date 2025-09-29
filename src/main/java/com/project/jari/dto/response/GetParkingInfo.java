package com.project.jari.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GetParkingInfo {
    @JsonProperty("list_total_count")
    private int listTotalCount;

    @JsonProperty("RESULT")
    private ApiResult result;

    @JsonProperty("row")
    private List<ParkingLotInfo> row;
}
