package com.woorido.account.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TransactionSearchRequest {
    private String type; // TransactionType Enum 문자열
    private String startDate; // YYYY-MM-DD
    private String endDate; // YYYY-MM-DD
    private Integer page = 0;
    private Integer size = 20;

    // accountId는 서비스에서 주입
    private String accountId;
}
