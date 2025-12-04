package com.project.jari.entity.login;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 ID (Member의 mbId)
     * unique = true: 한 사용자당 하나의 Refresh Token만 존재
     */
    @Column(nullable = false, unique = true, length = 50)
    private String mbId;

    // Refresh Token 값
    @Column(nullable = false, length = 500)
    private String token;

    // 만료시간
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // 생성시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    // 유틸리티 메소드들

    // 생성시간 만들때 사용하는 내부메소드
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 토큰이 만료되엉ㅆ는지
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    // 재로그인시 토큰 업데이트
    public void updateToken(String newToken, LocalDateTime newExpiryDate) {
        this.token = newToken;
        this.expiryDate = newExpiryDate;
    }
}
