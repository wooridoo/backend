package com.woorido.auth.social;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.woorido.common.entity.SocialProvider;

@Component
public class GoogleSocialOAuthClient extends AbstractSocialOAuthClient {
    private static final String AUTHORIZE_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleSocialOAuthClient(
            @Value("${oauth.google.client-id:}") String clientId,
            @Value("${oauth.google.client-secret:}") String clientSecret,
            @Value("${oauth.google.redirect-uri:}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(redirectUri);
    }

    @Override
    public String defaultRedirectUri() {
        return redirectUri;
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public SocialUserProfile fetchUserProfile(String authorizationCode) {
        Map<String, String> tokenForm = new LinkedHashMap<>();
        tokenForm.put("grant_type", "authorization_code");
        tokenForm.put("client_id", clientId);
        tokenForm.put("client_secret", clientSecret);
        tokenForm.put("redirect_uri", redirectUri);
        tokenForm.put("code", authorizationCode);

        JsonNode tokenResponse = postForm(TOKEN_ENDPOINT, tokenForm);
        String accessToken = tokenResponse.path("access_token").asText(null);
        if (!StringUtils.hasText(accessToken)) {
            throw new RuntimeException("AUTH_014:소셜 인증 처리에 실패했습니다");
        }

        JsonNode profile = getJson(USER_INFO_ENDPOINT, accessToken);
        String socialId = profile.path("sub").asText(null);
        String email = profile.path("email").asText(null);
        String name = profile.path("name").asText(null);
        String picture = profile.path("picture").asText(null);

        return SocialUserProfile.builder()
                .provider(SocialProvider.GOOGLE)
                .socialId(socialId)
                .email(email)
                .nickname(name)
                .name(name)
                .profileImageUrl(StringUtils.hasText(picture) ? picture : null)
                .build();
    }
}
