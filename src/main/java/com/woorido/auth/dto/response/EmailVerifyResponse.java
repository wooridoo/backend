package com.woorido.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerifyResponse {
  private String email;
  private int expiresIn;
}
