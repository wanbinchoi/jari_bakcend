package com.project.jari.service.join;

import com.project.jari.config.JwtTokenProvider;
import com.project.jari.dto.join.MemberJoinRequest;
import com.project.jari.dto.login.LoginRequestDto;
import com.project.jari.dto.login.LoginResponseDto;
import com.project.jari.entity.join.Member;
import com.project.jari.entity.login.RefreshToken;
import com.project.jari.repository.join.MemberRepository;
import com.project.jari.repository.login.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;  // 토큰 유효시간 (밀리초)

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    //회원가입
    @Transactional
    public void join(MemberJoinRequest req) {

        // 1. 아이디 중복 체크
        if (memberRepository.existsByMbId(req.getMbId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(req.getMbPwd());

        // 3. member 엔티티 생성
        Member member = Member.builder()
                .mbId(req.getMbId())
                .mbPwd(encodedPassword)
                .mbNm(req.getMbNm())
                .mbTel(req.getMbTel())
                .build();

        // 4. DB 저장
        memberRepository.save(member);
    }

    // 로그인(refreshToken DB에 저장하는 로직 추가)
    @Transactional // DB저장 로직 추가했으므로 @Transactional 추가
    public LoginResponseDto login(LoginRequestDto req) {
        // 1. 아이디로 회원 조회
        Member member = memberRepository.findByMbId(req.getMbId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(req.getMbPwd(), member.getMbPwd())) {
            throw new IllegalArgumentException(("비밀번호가 일치하지 않습니다."));
        }

        // jwt 토큰 발급 (access token, refresh token)
        String accessToken = jwtTokenProvider.createAccessToken(member.getMbId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMbId());

        saveOrUpdateRefreshToken(member.getMbId(),refreshToken);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)  // 밀리초를 초로 변환
                .mbId(member.getMbId())
                .mbNm(member.getMbNm())
                .build();
    }

    /**
     * 로그아웃
     * DB에서 Refresh Token 삭제
     */
    @Transactional
    public void logout(String mbId) {
        refreshTokenRepository.deleteByMbId(mbId);
        System.out.println("로그아웃 성공 : " + mbId);
    }

    /**
     * Refresh Token 저장 또는 업데이트
     * - 기존 토큰이 있으면 업데이트
     * - 없으면 새로 저장
     */
    @Transactional
    private void saveOrUpdateRefreshToken(String mbId, String token) {
        // 만료 시간 계산 (현재 시간 + 유효기간)
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        // 기존 Refresh Token 조회
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByMbId(mbId);

        if (existingToken.isPresent()) {
            // 기존 토큰이 있으면 업데이트
            RefreshToken refreshTokenEntity = existingToken.get();
            refreshTokenEntity.updateToken(token, expiryDate);
            refreshTokenRepository.save(refreshTokenEntity);

            System.out.println("기존 Refresh Token 업데이트: " + mbId);
        } else {
            // 기존 토큰이 없으면 새로 생성
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .mbId(mbId)
                    .token(token)
                    .expiryDate(expiryDate)
                    .build();
            refreshTokenRepository.save(newRefreshToken);

            System.out.println("새 Refresh Token 저장: " + mbId);
        }
    }
}
