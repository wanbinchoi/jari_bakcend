package com.project.jari.dto;

import com.project.jari.entity.ParkingLot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 주차장 DTO (단순화 버전)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLotDto {
    
    // 기본 정보
    private String pkltCode;
    private String name;
    private String address;
    private String parkingType;
    private String operationType;
    
    // 위치 정보
    private Double latitude;
    private Double longitude;
    
    // 주차 용량
    private Integer totalCapacity;
    
    // 요금 정보
    private Boolean isPaid;
    private Integer baseRate;
    private Integer baseTime;
    private Integer additionalRate;
    private Integer additionalTime;
    private Integer dayMaxRate;
    
    // 운영 시간
    private Map<String, Object> operationHours;
    
    // 기타
    private String tel;
    private Boolean isShared;
    
    // 메타 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Entity → DTO 변환
     */
    public static ParkingLotDto from(ParkingLot entity) {
        return ParkingLotDto.builder()
                .pkltCode(entity.getPkltCode())
                .name(entity.getName())
                .address(entity.getAddress())
                .parkingType(entity.getParkingType())
                .operationType(entity.getOperationType())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .totalCapacity(entity.getTotalCapacity())
                .isPaid(entity.getIsPaid())
                .baseRate(entity.getBaseRate())
                .baseTime(entity.getBaseTime())
                .additionalRate(entity.getAdditionalRate())
                .additionalTime(entity.getAdditionalTime())
                .dayMaxRate(entity.getDayMaxRate())
                .operationHours(entity.getOperationHours())
                .tel(entity.getTel())
                .isShared(entity.getIsShared())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
