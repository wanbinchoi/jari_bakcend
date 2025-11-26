package com.project.jari.dto.join;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberJoinRequest {
    private String mbId;
    private String mbPwd;
    private String mbNm;
    private String mbTel;

}
