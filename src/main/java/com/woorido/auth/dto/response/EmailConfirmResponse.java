package com.woorido.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailConfirmResponse {
  private String verificationToken;
  private int expiresIn;
}
