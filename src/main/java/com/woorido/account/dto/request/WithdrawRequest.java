package com.woorido.account.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawRequest {
    private Long amount;
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
}
