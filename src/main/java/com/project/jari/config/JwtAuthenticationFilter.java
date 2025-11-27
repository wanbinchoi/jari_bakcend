package com.project.jari.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

       // 1. 요청 헤더에서 JWT 토큰 추출
        String token = getTokenFromRequest(request);
        System.out.println("추출된 토큰: " + token);  // ← 로그 추가

        // 2. 토큰이 있고 유효한지 검증
        if (token != null && jwtTokenProvider.validateToken(token)) {
            System.out.println("토큰 검증 성공!");  // ← 로그 추가

            // 3. 토큰에서 사용자 ID 추출
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            System.out.println("추출된 userId: " + userId);  // ← 로그 추가

            // 4. 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,        // principal: 사용자 ID
                            null,          // credentials: 비밀번호 (토큰 인증이므로 불필요)
                            null           // authorities: 권한 (나중에 추가 가능)
                    );

            // 5. 요청 정보 추가
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // 6. SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 7. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    // 요청 헤더에서 토큰 추출하는 메서드
    private String getTokenFromRequest(HttpServletRequest request) {
        // Authorization 헤더 가져오기
        String bearerToken = request.getHeader("Authorization");

        // "Bearer " 접두사가 있으면 토큰만 추출
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // "Bearer " 이후 문자열 반환
        }

        return null;
    }
}
