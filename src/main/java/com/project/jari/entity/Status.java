package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Status")
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long stCode;

    @OneToOne
    @JoinColumn(name = "Parking_lot")
    private ParkingLot pkltCode;

    private int vhclCnt;

    private LocalDateTime updtTm;
}
