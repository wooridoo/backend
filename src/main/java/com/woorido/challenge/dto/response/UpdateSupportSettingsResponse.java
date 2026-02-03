package com.woorido.challenge.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateSupportSettingsResponse {
    private String challengeId;
    private Boolean autoPayEnabled;
    private String nextPaymentDate;
    private Long amount;
}
