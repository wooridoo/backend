package com.woorido.meeting.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceResponseRequest {
  private String status; // CONFIRMED, DECLINED
  private String reason; // Optional
}
