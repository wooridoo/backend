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
public class MeetingVote {
  private String id;
  private String meetingId;
  private Integer requiredCount;
  private Integer attendCount;
  private Integer absentCount;
  private String status; // PENDING, APPROVED, REJECTED, EXPIRED
  private Integer version;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
  private LocalDateTime closedAt;
}
