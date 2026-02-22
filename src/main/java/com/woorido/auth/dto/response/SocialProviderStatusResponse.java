package com.woorido.auth.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialProviderStatusResponse {
    private List<ProviderStatus> providers;

    @Getter
    @Builder
    public static class ProviderStatus {
        private String provider;
        private boolean enabled;
        private String reasonCode;
    }
}
