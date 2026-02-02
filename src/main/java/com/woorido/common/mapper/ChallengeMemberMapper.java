package com.woorido.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChallengeMemberMapper {

    // 사용자가 참여 중인 활성 챌린지 수 조회 (탈퇴 여부 확인용)
    // left_at IS NULL 인 경우 참여 중으로 간주
    int countActiveMembershipsByUserId(@Param("userId") String userId);

    // 사용자가 완주한 챌린지 수 조회
    int countCompletedChallengesByUserId(@Param("userId") String userId);

    // 공통 챌린지 조회 (현재 사용자와 대상 사용자가 함께 참여 중인 챌린지)
    java.util.List<java.util.Map<String, Object>> findCommonChallenges(@Param("currentUserId") String currentUserId,
            @Param("targetUserId") String targetUserId);
}
