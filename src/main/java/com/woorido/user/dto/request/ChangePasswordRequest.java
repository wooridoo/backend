package com.woorido.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword; // 8-20자, 영문+숫자+특수문자
    private String newPasswordConfirm;
}
