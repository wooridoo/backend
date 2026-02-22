package com.woorido.auth.social;

import com.woorido.common.entity.SocialProvider;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialUserProfile {
    private SocialProvider provider;
    private String socialId;
    private String email;
    private String nickname;
    private String name;
    private String profileImageUrl;
}

