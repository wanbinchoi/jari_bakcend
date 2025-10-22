package com.project.jari.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kakao Map API 주소 검색 응답 DTO
 * API 문서: https://developers.kakao.com/docs/latest/ko/local/dev-guide#address-coord
 */
@Getter
@NoArgsConstructor
public class KakaoAddressResponse {

    private Meta meta;
    private List<Document> documents;

    @Getter
    @NoArgsConstructor
    public static class Meta {
        @JsonProperty("total_count")
        private Integer totalCount;
    }

    @Getter
    @NoArgsConstructor
    public static class Document {
        @JsonProperty("address_name")
        private String addressName;

        /**
         * 경도 (Longitude)
         * 주의: Kakao API는 x=경도, y=위도 순서
         */
        private String x;

        /**
         * 위도 (Latitude)
         */
        private String y;

        /**
         * 경도를 Double로 변환
         */
        public Double getLongitude() {
            try {
                return Double.parseDouble(x);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /**
         * 위도를 Double로 변환
         */
        public Double getLatitude() {
            try {
                return Double.parseDouble(y);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * 응답에서 첫 번째 좌표 추출
     * @return 좌표 배열 [위도, 경도] 또는 null
     */
    public Double[] getFirstCoordinate() {
        if (documents == null || documents.isEmpty()) {
            return null;
        }

        Document doc = documents.get(0);
        Double latitude = doc.getLatitude();
        Double longitude = doc.getLongitude();

        if (latitude == null || longitude == null) {
            return null;
        }

        return new Double[]{latitude, longitude};
    }
}
