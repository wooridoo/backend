package com.woorido.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class UserPublicProfileResponse {
    private Long userId;
    private String nickname;
    private String profileImage;
    private Double brix;
    private Stats stats;
    private List<Map<String, Object>> commonChallenges;
    private Boolean isVerified;
    private String createdAt;

    @Getter
    @Builder
    public static class Stats {
        private int completedChallenges;
        private int totalMeetings;
    }
}
