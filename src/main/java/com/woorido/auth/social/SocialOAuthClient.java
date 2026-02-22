package com.woorido.auth.social;

import com.woorido.common.entity.SocialProvider;

public interface SocialOAuthClient {
    SocialProvider provider();

    boolean isConfigured();

    String defaultRedirectUri();

    String buildAuthorizeUrl(String state);

    SocialUserProfile fetchUserProfile(String authorizationCode);
}
