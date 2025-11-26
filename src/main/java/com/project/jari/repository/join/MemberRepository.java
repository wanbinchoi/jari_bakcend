package com.project.jari.repository.join;

import com.project.jari.entity.join.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByMbId(String mbId); //아이디 중복체크


}
