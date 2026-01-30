package com.woorido.auth.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequest {
    private String email;
    private String type; // SIGNUP | PASSWORD_RESET
}
