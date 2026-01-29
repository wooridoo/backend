package com.woorido.account.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawResponse {
    private Long withdrawId;
    private Long amount;
    private Long fee;
    private Long netAmount;
    private Long newBalance;
    private BankInfo bankInfo;
    private String estimatedArrival;
    private String createdAt;

    @Getter
    @Builder
    public static class BankInfo {
        private String bankCode;
        private String bankName;
        private String accountNumber;
    }
}
