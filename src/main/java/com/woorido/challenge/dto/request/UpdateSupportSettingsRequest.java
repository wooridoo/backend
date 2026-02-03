package com.woorido.challenge.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateSupportSettingsRequest {
    @NotNull(message = "VALIDATION_001: 자동 납입 설정 여부는 필수입니다")
    private Boolean autoPayEnabled;
}
