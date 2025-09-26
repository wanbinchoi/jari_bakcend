package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Fee")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long fCode;

    @OneToOne
    @JoinColumn(name = "Paring_lot")
    private ParkingLot pkltCode;

    private String lat;

    private String lot;

}
