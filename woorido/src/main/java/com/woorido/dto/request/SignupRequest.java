package com.woorido.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

  private String email;
  private String password;
  private String nickname;
  private String phone;
  private String birthDate;
  private String name;
  private String verificationToken;
  private Boolean termsAgreed;
  private Boolean privacyAgreed;
  private Boolean marketingAgreed;
}
