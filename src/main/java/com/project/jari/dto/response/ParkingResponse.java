package com.project.jari.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ParkingResponse {
    @JsonProperty("GetParkingInfo")
    private GetParkingInfo getParkingInfo;
}
