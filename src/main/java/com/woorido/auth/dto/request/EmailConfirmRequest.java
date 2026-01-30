package com.woorido.auth.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailConfirmRequest {
    private String email;
    private String code;
}
