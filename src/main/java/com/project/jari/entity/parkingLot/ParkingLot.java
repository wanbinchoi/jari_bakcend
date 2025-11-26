package com.project.jari.entity.parkingLot;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 주차장 기본 정보 엔티티 (단순화 버전)
 * Location, Fee, Management, Operation 테이블을 통합한 단일 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "parking_lot", indexes = {
        @Index(name = "idx_location", columnList = "latitude, longitude"),
        @Index(name = "idx_name", columnList = "name")
})
public class ParkingLot {

    // ===== pk =====
    @Id
    @Column(name = "pklt_code", length = 20)
    private String pkltCode;

    // ===== 기본 정보 =====
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "address", length = 200, nullable = false)
    private String address;

    @Column(name = "parking_type", length = 20)
    private String parkingType;

    @Column(name = "operation_type", length = 50)
    private String operationType;

    // ===== 위치 정보 =====
    @Column(name = "latitude", precision = 10, nullable = false)
    private Double latitude;

    @Column(name = "longitude", precision = 11, nullable = false)
    private Double longitude;

    // ===== 주차 정보 =====
    @Column(name = "total_capacity")
    @Builder.Default
    private Integer totalCapacity = 0;

    @Column(name = "now_vhcl_cnt")
    @Builder.Default
    private Integer nowVhclCnt = 0;

    // ===== 요금 정보 =====
    @Column(name = "is_paid")
    @Builder.Default
    private Boolean isPaid = true;

    @Column(name = "base_rate")
    @Builder.Default
    private Integer baseRate = 0;

    @Column(name = "base_time")
    @Builder.Default
    private Integer baseTime = 0;

    @Column(name = "additional_rate")
    @Builder.Default
    private Integer additionalRate = 0;

    @Column(name = "additional_time")
    @Builder.Default
    private Integer additionalTime = 0;

    @Column(name = "day_max_rate")
    @Builder.Default
    private Integer dayMaxRate = 0;

    // ===== 운영 시간 (JSON) =====
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operation_hours", columnDefinition = "json")
    private Map<String, Object> operationHours;

    // ===== 기타 정보 =====
    @Column(name = "tel", length = 20)
    private String tel;

    @Column(name = "is_shared")
    @Builder.Default
    private Boolean isShared = false;

    // ===== 메타 정보 =====
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== 비즈니스 메서드 =====

    /**
     * 정보 업데이트
     */
    public void updateInfo(String name, String address, Integer totalCapacity) {
        this.name = name;
        this.address = address;
        this.totalCapacity = totalCapacity;
        this.updatedAt = LocalDateTime.now();
    }

}

