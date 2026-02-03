package com.woorido.challenge.dto.response;

import java.util.List;

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
public class ChallengeMemberDetailResponse {
    private String memberId;
    private UserInfo user;
    private String role;
    private String status;
    private Stats stats;
    private List<SupportHistory> supportHistory;
    private String joinedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String userId;
        private String nickname;
        private String profileImage;
        private Double brix;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private Long totalSupport;
        private Double supportRate;
        private Double attendanceRate;
        private Integer meetingsAttended;
        private Integer meetingsTotal;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportHistory {
        private String month;
        private Long amount;
        private String paidAt;
    }
}
