package com.woorido.challenge.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeetingMapper {
    /**
     * 챌린지의 완료된(COMPLETED) 총 정기모임 횟수 조회
     */
    int countTotalMeetings(@Param("challengeId") String challengeId);

    /**
     * 사용자가 'ATTENDED' 상태로 참여한 정기모임 횟수 조회
     */
    int countAttendedMeetings(@Param("challengeId") String challengeId, @Param("userId") String userId);
}
