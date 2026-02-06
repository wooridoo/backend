package com.woorido.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserWithdrawRequest {
    private String password;
    private String reason;
}
