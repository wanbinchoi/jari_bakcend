package com.project.jari.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApiResult {
    @JsonProperty("CODE")
    private String code;

    @JsonProperty("MESSAGE")
    private String message;
}