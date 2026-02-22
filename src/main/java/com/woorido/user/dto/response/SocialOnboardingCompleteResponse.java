package com.woorido.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialOnboardingCompleteResponse {
    private boolean completed;
    private UserProfileResponse user;
}
