package com.woorido.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequest {

  @NotBlank(message = "리프레시 토큰은 필수입니다")
  private String refreshToken;
}
