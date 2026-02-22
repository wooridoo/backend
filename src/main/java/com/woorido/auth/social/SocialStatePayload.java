package com.woorido.auth.social;

import com.woorido.common.entity.SocialProvider;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialStatePayload {
    private SocialProvider provider;
    private SocialAuthIntent intent;
    private String returnTo;
}
