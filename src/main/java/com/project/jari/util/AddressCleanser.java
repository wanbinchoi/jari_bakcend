package com.project.jari.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주소 데이터 정제 유틸리티
 *
 * 면접 포인트:
 * - "데이터 품질 향상을 위한 전처리 로직을 구현했습니다"
 * - "정규식을 활용하여 다양한 주소 패턴을 처리했습니다"
 * - "단일 책임 원칙(SRP)을 적용하여 주소 정제 로직을 분리했습니다"
 */
@Slf4j
@Component
public class AddressCleanser {

    /**
     * 주소를 지오코딩에 적합한 형태로 정제
     *
     * @param rawAddress 원본 주소
     * @return 정제된 주소
     */
    public String cleanseAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.trim().isEmpty()) {
            return null;
        }

        String cleaned = rawAddress;

        // 1. 앞뒤 공백 제거
        cleaned = cleaned.trim();

        // 2. 연속된 공백을 하나로 통일
        cleaned = cleaned.replaceAll("\\s+", " ");

        // 3. 주소 끝에 붙은 " 0" 제거 ⭐ 핵심!
        // 예: "서울특별시 강남구 테헤란로 152 0" -> "서울특별시 강남구 테헤란로 152"
        cleaned = cleaned.replaceAll("\\s+0$", "");

        // 4. 괄호 안 내용 제거
        // 예: "역삼동(강남구)" -> "역삼동"
        cleaned = cleaned.replaceAll("\\([^)]*\\)", "");

        // 5. 지하, 층수 정보 제거
        // 예: "지하1층", "3층" 등
        cleaned = cleaned.replaceAll("지하\\d+층?|\\d+층", "");

        // 6. 불필요한 건물 관련 단어 제거
        cleaned = cleaned.replaceAll("(빌딩|건물|타워)", "");

        // 7. "서울시" -> "서울특별시" 변환 (Kakao API가 선호)
        cleaned = cleaned.replace("서울시", "서울특별시");

        // 8. 최종 공백 정리
        cleaned = cleaned.trim();

        // 로깅 (변경사항이 있을 경우만)
        if (!rawAddress.equals(cleaned)) {
            log.debug("주소 정제: '{}' -> '{}'", rawAddress, cleaned);
        }

        return cleaned;
    }

    /**
     * 도로명 주소만 추출 (구주소 제거)
     *
     * @param address 원본 주소
     * @return 도로명 주소
     */
    public String extractRoadAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return address;
        }

        // "역삼동 123-45"와 같은 구주소 패턴 제거
        // 패턴: "동/리/가" + 숫자-숫자
        String pattern = "\\s*[동리가]\\s*\\d+-?\\d*";
        String cleaned = address.replaceAll(pattern, "").trim();

        log.debug("도로명 주소 추출: '{}' -> '{}'", address, cleaned);

        return cleaned;
    }
}
