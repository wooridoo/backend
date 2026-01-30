package com.woorido.challenge.repository;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.challenge.domain.ChallengeMember;

@Mapper
public interface ChallengeMemberMapper {

        // 챌린지 멤버 등록
        void insert(ChallengeMember member);

        // 사용자의 챌린지 멤버십 조회
        Map<String, Object> findByUserIdAndChallengeId(
                        @Param("userId") String userId,
                        @Param("challengeId") String challengeId);

        // 챌린지 전체 멤버 조회
        List<ChallengeMember> findAllByChallengeId(@Param("challengeId") String challengeId);

        // 챌린지 멤버 탈퇴 처리
        int updateLeaveMember(@Param("userId") String userId, @Param("challengeId") String challengeId,
                        @Param("leaveReason") String leaveReason);

        // 챌린지 멤버 재가입 처리
        int updateRejoinMember(ChallengeMember member);

        // 멤버 정보 포함 조회 (API 032)
        List<Map<String, Object>> findMembersWithUserInfo(@Param("challengeId") String challengeId);
}
