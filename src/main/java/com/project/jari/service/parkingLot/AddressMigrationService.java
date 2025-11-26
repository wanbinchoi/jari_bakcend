package com.project.jari.service.parkingLot;

import com.project.jari.entity.parkingLot.ParkingLot;
import com.project.jari.repository.parkingLot.ParkingLotRepository;
import com.project.jari.util.AddressCleanser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 기존 DB 데이터를 정제하는 마이그레이션 서비스
 *
 * 면접 포인트:
 * - "데이터 품질 개선을 위한 마이그레이션 스크립트를 작성했습니다"
 * - "이미 저장된 데이터의 정합성을 유지하기 위해 배치 처리를 구현했습니다"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressMigrationService {

    private final ParkingLotRepository parkingLotRepository;
    private final AddressCleanser addressCleanser;

    /**
     * 기존 DB 주소 데이터 일괄 정제
     *
     * @return 정제된 레코드 수
     */
    @Transactional
    public int cleanseExistingAddresses() {
        log.info("=== 기존 주소 데이터 정제 시작 ===");

        List<ParkingLot> allParkingLots = parkingLotRepository.findAll();
        AtomicInteger updatedCount = new AtomicInteger(0);

        allParkingLots.forEach(parkingLot -> {
            String originalAddress = parkingLot.getAddress();
            String cleansedAddress = addressCleanser.cleanseAddress(originalAddress);

            // 주소가 변경된 경우만 업데이트
            if (!originalAddress.equals(cleansedAddress)) {
                log.info("주소 정제: '{}' -> '{}'", originalAddress, cleansedAddress);

                // ParkingLot 엔티티에 updateInfo 메서드가 있다면:
                parkingLot.updateInfo(
                        parkingLot.getName(),
                        cleansedAddress,  // 정제된 주소
                        parkingLot.getTotalCapacity()
                );

                parkingLotRepository.save(parkingLot);
                updatedCount.incrementAndGet();
            }
        });

        log.info("=== 주소 정제 완료: {}건 업데이트됨 ===", updatedCount.get());
        return updatedCount.get();
    }

    /**
     * 특정 패턴의 주소만 정제 (예: 끝에 "0"이 붙은 경우만)
     *
     * @return 정제된 레코드 수
     */
    @Transactional
    public int cleanseAddressesEndingWithZero() {
        log.info("=== 끝에 '0'이 붙은 주소 정제 시작 ===");

        List<ParkingLot> allParkingLots = parkingLotRepository.findAll();
        AtomicInteger updatedCount = new AtomicInteger(0);

        allParkingLots.stream()
                .filter(parkingLot -> parkingLot.getAddress().endsWith(" 0"))  // " 0"으로 끝나는 것만
                .forEach(parkingLot -> {
                    String originalAddress = parkingLot.getAddress();
                    String cleansedAddress = addressCleanser.cleanseAddress(originalAddress);

                    log.info("주소 정제: '{}' -> '{}'", originalAddress, cleansedAddress);

                    parkingLot.updateInfo(
                            parkingLot.getName(),
                            cleansedAddress,
                            parkingLot.getTotalCapacity()
                    );

                    parkingLotRepository.save(parkingLot);
                    updatedCount.incrementAndGet();
                });

        log.info("=== '0' 제거 완료: {}건 업데이트됨 ===", updatedCount.get());
        return updatedCount.get();
    }
}