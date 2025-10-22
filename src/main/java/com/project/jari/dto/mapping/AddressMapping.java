package com.project.jari.dto.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주소 매핑 정보 DTO
 * 
 * 면접 포인트:
 * - "API 변환에 실패한 주소들을 수동으로 관리하기 위한 매핑 시스템을 구현했습니다"
 * - "JSON 파일을 활용하여 설정 파일을 외부화했습니다"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressMapping {
    
    /**
     * API에서 받아온 원본 주소 (변환 실패한 주소)
     */
    @JsonProperty("originalAddress")
    private String originalAddress;
    
    /**
     * 수동으로 확인한 올바른 주소
     */
    @JsonProperty("correctAddress")
    private String correctAddress;
    
    /**
     * 도로명 주소 형태
     */
    @JsonProperty("roadNameAddress")
    private String roadNameAddress;
    
    /**
     * 위도
     */
    @JsonProperty("latitude")
    private Double latitude;
    
    /**
     * 경도
     */
    @JsonProperty("longitude")
    private Double longitude;
    
    /**
     * 검증한 소스 (naver_map, google_map, manual_verification)
     */
    @JsonProperty("verificationSource")
    private String verificationSource;
    
    /**
     * 마지막 검증 날짜
     */
    @JsonProperty("lastVerified")
    private String lastVerified;
    
    /**
     * 추가 설명
     */
    @JsonProperty("notes")
    private String notes;
    
    /**
     * 좌표 배열로 반환 (카카오 API 응답 형태와 동일)
     */
    public Double[] getCoordinatesArray() {
        return new Double[]{latitude, longitude};
    }
    
    /**
     * 원본 주소와 매칭되는지 확인
     */
    public boolean matches(String targetAddress) {
        if (targetAddress == null || originalAddress == null) {
            return false;
        }
        
        // 대소문자 무시하고 공백 정리 후 비교
        String normalized1 = targetAddress.trim().toLowerCase().replaceAll("\\s+", " ");
        String normalized2 = originalAddress.trim().toLowerCase().replaceAll("\\s+", " ");
        
        return normalized1.equals(normalized2);
    }
}
