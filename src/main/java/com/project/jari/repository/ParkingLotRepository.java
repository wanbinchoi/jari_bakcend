package com.project.jari.repository;

import com.project.jari.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, String> {

    // 추가 메소드들
    // 이름으로 검색
    List<ParkingLot> findByNameContaining(String name);
    // 주소로 검색
    List<ParkingLot> findByAddressContaining(String address);
}
