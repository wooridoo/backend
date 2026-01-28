package com.woorido.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserUpdateResponse {

  private Long userId;
  private String nickname;
  private String phone;
  private String profileImage;
  private String updatedAt;
}
