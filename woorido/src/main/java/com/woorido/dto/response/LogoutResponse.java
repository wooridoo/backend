package com.woorido.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LogoutResponse {

  private boolean loggedOut;

  public static LogoutResponse success() {
    return LogoutResponse.builder()
        .loggedOut(true)
        .build();
  }
}
