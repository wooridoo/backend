package com.woorido.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RefreshResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private int expiresIn;

  public static RefreshResponse of(String accessToken, String refreshToken, int expiresIn) {
    return RefreshResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(expiresIn)
        .build();
  }
}
