package com.project.jari.repository.parkingLot;

import com.project.jari.entity.parkingLot.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, String> {

    // 추가 메소드들
    // 이름으로 검색
    List<ParkingLot> findByNameContaining(String name);
    // 주소로 검색
    List<ParkingLot> findByAddressContaining(String address);

    // @Query 사용해서
    @Query("SELECT p FROM ParkingLot p WHERE " +
            "p.name LIKE %:keyword% OR p.address LIKE %:keyword%")
    List<ParkingLot> searchByKeyword(@Param("keyword") String keyword);
}
