package com.woorido.account.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChargeCallbackRequest {
    private String orderId;
    private String paymentKey;
    private Long amount;
    private String status;
}
