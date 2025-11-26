package com.project.jari.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mb_code")
    private Long mbCode;

    @Column(name = "mb_id", unique = true, nullable = false, length = 20)
    private String mbId;

    @Column(name = "mb_pwd", nullable = false, length = 255)
    private String mbPwd;

    @Column(name = "mb_nm", length = 20)
    private String mbNm;

    @Column(name = "mb_tel", length = 20)
    private String mbTel;

    @Column(name = "mb_reg", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime mbReg = LocalDateTime.now();
}
