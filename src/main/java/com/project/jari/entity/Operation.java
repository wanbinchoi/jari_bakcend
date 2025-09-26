package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Operation")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long opCode;

    @ManyToOne
    @JoinColumn(name = "Day_type")
    private DayType dtCode;

    @OneToOne
    @JoinColumn(name = "Parking_lot")
    private ParkingLot pkltCode;

    private LocalTime beginTm;

    private LocalTime endTm;
}
