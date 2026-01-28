package com.woorido.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfo {

    private String userId;
    private String email;
    private String nickname;
    private String profileImage;
    private String status;
    private Boolean isNewUser;
}
