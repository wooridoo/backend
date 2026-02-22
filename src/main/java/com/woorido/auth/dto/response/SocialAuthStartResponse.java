package com.woorido.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialAuthStartResponse {
    private String authorizeUrl;
}

