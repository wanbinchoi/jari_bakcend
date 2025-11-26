package com.project.jari.service.join;

import com.project.jari.dto.join.MemberJoinRequest;
import com.project.jari.dto.login.LoginRequestDto;
import com.project.jari.entity.join.Member;
import com.project.jari.repository.join.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    //회원가입
    @Transactional
    public void join(MemberJoinRequest req){

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
    public Member login(LoginRequestDto req){
        // 1. 아이디로 회원 조회
        Member member = memberRepository.findByMbId(req.getMbId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        // 2. 비밀번호 확인
        if(!passwordEncoder.matches(req.getMbPwd(), member.getMbPwd())){
            throw new IllegalArgumentException(("비밀번호가 일치하지 않습니다."));
        }
        return member;
    }

}
