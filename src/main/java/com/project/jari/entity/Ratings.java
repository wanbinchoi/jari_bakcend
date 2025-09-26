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
@Table(name = "Ratings")
public class Ratings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long raCode;

    @ManyToOne
    @JoinColumn(name = "Member")
    private Member mbCode;

    @ManyToOne
    @JoinColumn(name = "Parking_lot")
    private ParkingLot pkltCode;

    private int rating;

    private String comment;

    private LocalDateTime raReg;

}
