package com.project.jari.entity.review;

import com.project.jari.entity.join.Member;
import com.project.jari.entity.parkingLot.ParkingLot;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ratings")
public class Ratings {

    @Id
    @Column(name = "ra_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long raCode;

    @ManyToOne
    @JoinColumn(name = "mb_code")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "pklt_code")
    private ParkingLot parkingLot;

    @Column(name = "rating")
    private int rating;

    @Column(name = "comment", length = 200)
    private String comment;

    @Column(name = "ra_reg")
    @Builder.Default
    private LocalDateTime raReg = LocalDateTime.now();
}
