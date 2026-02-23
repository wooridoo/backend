package com.woorido.meeting.repository;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.meeting.domain.Meeting;

@Mapper
public interface MeetingMapper {

        // 모임 생성
        void insert(Meeting meeting);

        // 모임 목록 조회 (필터링, 페이징)
        List<Map<String, Object>> findAllByChallengeIdWithFilter(
                        @Param("challengeId") String challengeId,
                        @Param("status") String status,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        // 모임 총 개수 조회 (필터링)
        long countAllByChallengeIdWithFilter(
                        @Param("challengeId") String challengeId,
                        @Param("status") String status);

        // 모임 상세 조회
        Map<String, Object> findById(@Param("meetingId") String meetingId);

        // 모임 수정
        void update(Meeting meeting);

        // 모임 완료 처리
        void complete(Meeting meeting);

        // 모임 삭제
        void deleteById(@Param("meetingId") String meetingId);

        // 모임 참석자 수 조회
        int countAttendees(@Param("meetingId") String meetingId);

        // 모임 참석 여부 확인
        int isAttendee(@Param("meetingId") String meetingId, @Param("userId") String userId);

        int countActualAttendees(@Param("meetingId") String meetingId);

        int isActualAttendee(@Param("meetingId") String meetingId, @Param("userId") String userId);

        // 모임 참석자 목록 조회 (AGREE 상태)
        List<Map<String, Object>> findAttendeesByMeetingId(@Param("meetingId") String meetingId);

        int countCompletedMeetingsByChallengeId(@Param("challengeId") String challengeId);

        int countAttendedCompletedMeetingsByChallengeIdAndUserId(
                        @Param("challengeId") String challengeId,
                        @Param("userId") String userId);

        // 특정 유저가 최근 완료한 모임 수(리더 강퇴 활성 조건 확인용)
        int countCompletedMeetingsSince(@Param("challengeId") String challengeId,
                        @Param("userId") String userId,
                        @Param("since") LocalDateTime since);
}
