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
    @Column(name = "mb_code", length = 20)
    private String mbCode;

    @Column(name = "mb_id", length = 20)
    private String mbId;

    @Column(name = "mb_pwd", length = 20)
    private String mbPwd;

    @Column(name = "mb_nm", length = 20)
    private String mbNm;

    @Column(name = "mb_tel", length = 20)
    private String mbTel;

    @Column(name = "mb_reg")
    @Builder.Default
    private LocalDateTime mbReg = LocalDateTime.now();
}
