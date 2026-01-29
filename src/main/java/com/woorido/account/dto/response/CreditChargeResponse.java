package com.woorido.account.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditChargeResponse {
    private String orderId;
    private Long amount;
    private Long fee;
    private Long totalPaymentAmount;
    private String paymentUrl;
    private String expiresAt;
}
