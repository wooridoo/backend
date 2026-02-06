package com.woorido.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserWithdrawResponse {
    private String userId;
    private String status; // WITHDRAWN
    private String withdrawnAt;
    private String dataDeletedAt;
}
