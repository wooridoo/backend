package com.woorido.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NicknameCheckResponse {

  private String nickname;
  private Boolean available;

  public static NicknameCheckResponse available(String nickname) {
    return NicknameCheckResponse.builder()
        .nickname(nickname)
        .available(true)
        .build();
  }

  public static NicknameCheckResponse unavailable(String nickname) {
    return NicknameCheckResponse.builder()
        .nickname(nickname)
        .available(false)
        .build();
  }
}
