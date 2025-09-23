package com.project.jari.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class parkingLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int pkltCode;
}
