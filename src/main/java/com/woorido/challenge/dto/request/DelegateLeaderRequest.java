package com.woorido.challenge.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateLeaderRequest {
    private String targetUserId;
    private String targetMemberId; // For backward compatibility
}
