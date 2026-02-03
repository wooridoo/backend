package com.woorido.meeting.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingVoteRecord {
  private String id;
  private String meetingVoteId;
  private String userId;
  private String choice; // AGREE, DISAGREE
  private String actualAttendance; // PENDING, ATTENDED
  private LocalDateTime attendanceConfirmedAt;
  private LocalDateTime createdAt;
}
