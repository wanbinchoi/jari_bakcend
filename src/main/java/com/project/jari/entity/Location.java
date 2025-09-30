package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long lcCode;

    @OneToOne
    @JoinColumn(name = "Parking_lot")
    private ParkingLot pkltCode;

    private String lat;

    private  String lot;

}
