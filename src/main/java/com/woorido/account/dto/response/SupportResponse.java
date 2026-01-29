package com.woorido.account.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SupportResponse {
    private Long transactionId;
    private String challengeId;
    private String challengeName;
    private Long amount;
    private Long newBalance;
    private Long newChallengeBalance;
    private Boolean isFirstSupport;
    private String createdAt;
}
