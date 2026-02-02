package com.woorido.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeetingVoteRecordMapper {
    /**
     * 사용자가 참석한 총 모임 수 조회
     * 
     * @param userId 사용자 ID
     * @return 참석 횟수
     */
    int countAttendedMeetingsByUserId(@Param("userId") String userId);
}
