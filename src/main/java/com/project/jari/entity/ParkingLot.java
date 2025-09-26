package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Parking_lot")
public class ParkingLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long pkltCode;

    private String pkltNm;

    private String pkltAddr;

    private String pkltKnd;

    private String operSe;

    private int track;

    private String payYn;

    private String nightPayYn;

    private String satFreeSe;

    private String lhldyFreeSe;

}
