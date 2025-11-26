package com.project.jari.repository;

import com.project.jari.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByMbId(String mbId); //아이디 중복체크


}
