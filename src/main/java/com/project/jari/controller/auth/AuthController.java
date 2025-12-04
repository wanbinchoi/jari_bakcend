package com.project.jari.controller.auth;

import com.project.jari.config.JwtTokenProvider;
import com.project.jari.dto.login.LoginRequestDto;
import com.project.jari.dto.login.LoginResponseDto;
import com.project.jari.entity.login.RefreshToken;
import com.project.jari.repository.login.RefreshTokenRepository;
import com.project.jari.service.join.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;


import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final MemberService memberService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // login
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto req){
        try{
            LoginResponseDto res = memberService.login(req);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication){
        try{
            if (authentication == null) {
                System.out.println("Authentication이 null입니다!");
                return ResponseEntity.status(401).body("인증이 필요합니다.");
            }

            // security 사용자 이름 갖고오기
            String mbId = authentication.getName();
            System.out.println("사용자 ID: " + mbId);

            // DB에서 Refresh Token 삭제
            memberService.logout(mbId);

            log.info("{}님 로그아웃 완료", mbId);
            return ResponseEntity.ok("로그아웃 되셨습니다.");
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body("로그아웃 실패: " + e.getMessage());
        }
    }

    // Access Token이 만료되었을 때 Refresh Token으로 재발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request){
        try{
            String refreshToken = request.get("refreshToken");

            // 1. Refresh Token 검증
            if(!jwtTokenProvider.validateToken(refreshToken)){
                return ResponseEntity.status(401).body("유효하지 않은 Refresh Token 입니다.");
            }

            // 2. DB에서 Refresh Token 확인
            RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token 입니다."));

            // 3. 만료 확인
            if(storedToken.isExpired()){
                refreshTokenRepository.delete(storedToken);
                return ResponseEntity.status(401).body("만료된 Refresh Token입니다. 재로그인이 필요합니다.");
            }

            // 4. 사용자 Id 추출
            String mdId = jwtTokenProvider.getUserIdFromToken(refreshToken);

            // 5. 새 Access Token 발행
            String newAccessToken = jwtTokenProvider.createAccessToken(mdId);

            // 6. 응답
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expireIn", accessTokenExpiration/1000);

            return ResponseEntity.ok(response);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("Token 재발급 실패! 해당 메세지는 /n"+e.getMessage());
        }
    }
}

/*
🎯 최적의 구조(요즘 표준 Best Practice)
✔ Access Token → React 메모리에 저장

단, 새로고침하면 초기화됨 → refreshToken으로 재발급

✔ Refresh Token → HttpOnly Cookie + DB 저장

React는 쿠키를 직접 다루지 않고 브라우저가 요청마다 자동 첨부함.

이 구조가 보안도 좋고, 유지보수도 쉽고, 실제 기업들도 많이 사용함.
 */