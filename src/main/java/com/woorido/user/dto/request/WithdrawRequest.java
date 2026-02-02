package com.woorido.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawRequest {
    private String password; // 비밀번호 확인용
    private String reason; // 탈퇴 사유 (Optional)
}
