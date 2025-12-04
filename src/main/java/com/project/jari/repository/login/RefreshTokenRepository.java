package com.project.jari.repository.login;

import com.project.jari.entity.login.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 사용자 ID로 Refresh Token 조회
     * 로그인 시 기존 토큰 존재 여부 확인용
     */
    Optional<RefreshToken> findByMbId(String mbId);

    /**
     * Token 값으로 조회
     * Token 재발급 시 유효성 확인용
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 삭제
     * 로그아웃 시 사용
     */
    void deleteByMbId(String mbId);

    /**
     * Token 값으로 삭제
     * 로그아웃 시 사용 (대안)
     */
    void deleteByToken(String token);

    /**
     * 만료된 토큰 일괄 삭제
     * 배치 작업용 (선택적)
     */
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    /**
     * 사용자의 Refresh Token이 존재하는지 확인
     * 중복 로그인 체크용 (선택적)
     */
    boolean existsByMbId(String mbId);
}
