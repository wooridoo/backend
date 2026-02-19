package com.woorido.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPasswordChangeRequest {
  @NotBlank
  private String currentPassword;

  @NotBlank
  private String newPassword;

  @NotBlank
  private String newPasswordConfirm;
}
