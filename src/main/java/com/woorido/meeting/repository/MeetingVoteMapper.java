package com.woorido.meeting.repository;

import com.woorido.meeting.domain.MeetingVote;
import com.woorido.meeting.domain.MeetingVoteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface MeetingVoteMapper {
  // 투표 조회
  Optional<MeetingVote> findByMeetingId(String meetingId);

  // 투표 생성
  void insertVote(MeetingVote meetingVote);

  // 투표 기록 조회
  Optional<MeetingVoteRecord> findRecord(@Param("voteId") String voteId, @Param("userId") String userId);

  // 투표 기록 저장
  void insertRecord(MeetingVoteRecord record);

  // 투표 기록 수정 (참석 상태 변경 등)
  void updateRecord(MeetingVoteRecord record);

  int updateVoteStatus(@Param("voteId") String voteId, @Param("status") String status);

  int deleteRecordsByMeetingVoteId(@Param("voteId") String voteId);

  int deleteVoteById(@Param("voteId") String voteId);
}
