package com.woorido.meeting.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceResponseRequest {
  private String choice; // AGREE, DISAGREE
  private String status; // Deprecated backward compatibility
  private String reason; // Optional
}
