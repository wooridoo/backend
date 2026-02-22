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
}

