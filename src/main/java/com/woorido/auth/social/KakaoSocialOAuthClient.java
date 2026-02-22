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
public class KakaoSocialOAuthClient extends AbstractSocialOAuthClient {
    private static final String AUTHORIZE_ENDPOINT = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_ENDPOINT = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_ENDPOINT = "https://kapi.kakao.com/v2/user/me";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoSocialOAuthClient(
            @Value("${oauth.kakao.client-id:}") String clientId,
            @Value("${oauth.kakao.client-secret:}") String clientSecret,
            @Value("${oauth.kakao.redirect-uri:}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(clientId) && StringUtils.hasText(redirectUri);
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
                .queryParam("scope", "account_email profile_nickname")
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
        JsonNode account = profile.path("kakao_account");
        JsonNode accountProfile = account.path("profile");

        String socialId = profile.path("id").asText(null);
        String email = account.path("email").asText(null);
        String nickname = accountProfile.path("nickname").asText(null);
        String imageUrl = accountProfile.path("profile_image_url").asText(null);

        return SocialUserProfile.builder()
                .provider(SocialProvider.KAKAO)
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .name(nickname)
                .profileImageUrl(StringUtils.hasText(imageUrl) ? imageUrl : null)
                .build();
    }
}
