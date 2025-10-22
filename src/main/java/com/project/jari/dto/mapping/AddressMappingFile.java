package com.project.jari.dto.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 매핑 파일의 전체 구조를 나타내는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressMappingFile {
    
    /**
     * 매핑 데이터 리스트
     */
    @JsonProperty("mappings")
    private List<AddressMapping> mappings;
    
    /**
     * 메타데이터
     */
    @JsonProperty("metadata")
    private MappingMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappingMetadata {
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("lastUpdated")
        private String lastUpdated;
        
        @JsonProperty("totalMappings")
        private Integer totalMappings;
        
        @JsonProperty("description")
        private String description;
    }
}
