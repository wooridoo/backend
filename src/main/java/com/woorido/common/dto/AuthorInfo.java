package com.woorido.common.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthorInfo {
    private String userId;
    private String nickname;
    private String profileImage;
}
