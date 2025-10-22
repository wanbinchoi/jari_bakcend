package com.project.jari.controller;

import com.project.jari.service.AddressMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 주소 데이터 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/address")
@RequiredArgsConstructor
public class AddressManagementController {

    private final AddressMigrationService migrationService;

    /**
     * 기존 DB 주소 일괄 정제
     * GET /api/admin/address/cleanse
     */
    @PostMapping("/cleanse")
    public ResponseEntity<Map<String, Object>> cleanseAddresses() {
        log.info("주소 정제 API 호출됨");

        int updatedCount = migrationService.cleanseExistingAddresses();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "주소 정제 완료");
        response.put("updatedCount", updatedCount);

        return ResponseEntity.ok(response);
    }

    /**
     * 끝에 "0"이 붙은 주소만 정제
     * POST /api/admin/address/cleanse/zero
     */
    @PostMapping("/cleanse/zero")
    public ResponseEntity<Map<String, Object>> cleanseZeroSuffix() {
        log.info("'0' 제거 API 호출됨");

        int updatedCount = migrationService.cleanseAddressesEndingWithZero();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "주소 끝 '0' 제거 완료");
        response.put("updatedCount", updatedCount);

        return ResponseEntity.ok(response);
    }
}
