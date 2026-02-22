package com.woorido.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SocialAuthStartRequest {
    @NotBlank
    private String provider;

    @NotBlank
    private String intent;

    private String returnTo;

    /**
     * @deprecated 호환성 유지를 위한 필드입니다. 현재는 서버에서 무시됩니다.
     */
    @Deprecated(since = "2026-02", forRemoval = false)
    private String redirectUri;
}
