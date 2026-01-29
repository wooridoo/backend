package com.woorido.account.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChargeCallbackResponse {
    private Long transactionId;
    private String orderId;
    private Long amount;
    private Long newBalance;
    private String completedAt;
}
