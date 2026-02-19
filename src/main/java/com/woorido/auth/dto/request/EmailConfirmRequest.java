package com.woorido.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailConfirmRequest {
  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String code;
}
