package com.woorido.challenge.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.challenge.domain.Challenge;

@Mapper
public interface ChallengeMapper {
    Challenge findById(String id);

    // 챌린지 멤버 여부 확인 (존재하면 1, 아니면 0)
    int countMemberByChallengeIdAndUserId(@Param("challengeId") String challengeId, @Param("userId") String userId);

    // 챌린지 잔액 업데이트
    int updateBalance(Challenge challenge); // 반환값으로 낙관적 락 체크
}
