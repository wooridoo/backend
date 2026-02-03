package com.woorido.challenge.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeDeleteResponse {
    private String challengeId;
    private boolean deleted;
}
