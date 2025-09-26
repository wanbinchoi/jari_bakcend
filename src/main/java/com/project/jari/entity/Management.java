package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Management")
public class Management {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long mgCode;

    @OneToOne
    @JoinColumn(name = "Parking_lot")
    private ParkingLot pkltCode;

    private String mgTel;

    private String shrnYn;
}
