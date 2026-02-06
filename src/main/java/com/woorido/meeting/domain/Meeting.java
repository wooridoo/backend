package com.woorido.meeting.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {
  private String id;
  private String challengeId;
  private String title;
  private String description;
  private String location;
  private String locationDetail;
  // private String agenda; // 제거
  private LocalDateTime meetingDate; // scheduledAt -> meetingDate 변경
  private String status; // SCHEDULED, COMPLETED, CANCELED
  // private String beneficiaryId; // 제거
  // private Long benefitAmount; // 제거
  private String createdBy; // MemberID (Leader)
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime completedAt;
  // private String notes; // 제거
  // Benefit, Attendance 등의 정보는 별도 테이블이나 조인으로 처리
}
