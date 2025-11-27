package com.project.jari.controller.test;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // 인증이 필요한 API
    @GetMapping("/auth")
    public ResponseEntity<String> testAuth() {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 현재 로그인한 사용자 ID
        String userId = authentication.getPrincipal().toString();
        
        return ResponseEntity.ok("인증 성공! 현재 사용자: " + userId);
    }

    // 인증이 필요 없는 API (SecurityConfig에서 permitAll 설정 필요)
    @GetMapping("/public")
    public ResponseEntity<String> testPublic() {
        return ResponseEntity.ok("누구나 접근 가능한 API입니다.");
    }
}