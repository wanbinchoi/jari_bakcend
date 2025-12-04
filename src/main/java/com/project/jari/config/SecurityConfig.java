package com.project.jari.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /*
    * 회원가입 시 비밀번호를 암호화하고
    * 로그인 시 비교하는 데 사용
    */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // security filter chain으로 체인으로 연결된 필터를 적용하는 것임
        http
                // CSRF 비활성화 (JWT 사용 시 필요 없음)
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안 함 (JWT 방식은 Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로 (개발용 엔드포인트 설정)
                        .requestMatchers(
                                "/auth/login",// 로그인
                                "/auth/refresh",// access token 재발급
                                "/api/join/**",// 회원가입
                                "/api/test/**",//테스트
                                "/api/**"//테스트
                        ).permitAll()

                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터 추가
                // 요청 → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → 컨트롤러
                // JWT 필터가 먼저 실행되어 토큰 검증
                // 검증 성공 시 인증 정보 등록
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}