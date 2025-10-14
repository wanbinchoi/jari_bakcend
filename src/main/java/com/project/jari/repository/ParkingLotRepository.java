package com.project.jari.repository;

import com.project.jari.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, String> {
    
    // 주차장 코드 리스트로 조회
    List<ParkingLot> findByPkltCodeIn(List<String> codes);
    
    // 이름으로 검색
    List<ParkingLot> findByNameContaining(String name);
}
