package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Day_type")
public class DayType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long dtCode;

    private String dayOfWeek;
}
