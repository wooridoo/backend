package com.woorido.challenge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateLeaderResponse {
    private String challengeId;
    private MemberInfo previousLeader;
    private MemberInfo newLeader;
    private String delegatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private String memberId;
        private String userId;
        private String nickname;
        private String newRole;
    }
}
