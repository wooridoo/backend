package com.woorido.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialOnboardingRequest {
    private String nickname;
    private String phone;
    private boolean termsAgreed;
    private boolean privacyAgreed;
    private boolean marketingAgreed;
}
