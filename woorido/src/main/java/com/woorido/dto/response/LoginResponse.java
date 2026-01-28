package com.woorido.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private Integer expiresIn;
  private UserInfo user;
}
