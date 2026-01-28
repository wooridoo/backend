package com.woorido.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  private String id;
  private String email;
  private String passwordHash;
  private String name;
  private String nickname;
  private String phone;
  private String profileImageUrl;
  private LocalDate birthDate;
  private String gender;
  private String bio;
  private String isVerified;
  private String verificationToken;
  private LocalDateTime verificationTokenExpires;
  private String socialProvider;
  private String socialId;
  private String passwordResetToken;
  private LocalDateTime passwordResetExpires;
  private Integer failedLoginAttempts;
  private LocalDateTime lockedUntil;
  private String accountStatus;
  private LocalDateTime suspendedAt;
  private LocalDateTime suspendedUntil;
  private String suspensionReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime lastLoginAt;
  private String agreedTerms;
  private String agreedPrivacy;
  private String agreedMarketing;
  private LocalDateTime termsAgreedAt;

  // accountStatus의 별칭 메서드 (SignupResponse 호환)
  public String getStatus() {
    return this.accountStatus;
  }
}
