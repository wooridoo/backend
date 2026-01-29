package com.woorido.account.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreditChargeRequest {
    private Long amount;
    private String paymentMethod;
    private String returnUrl;
}
