package com.woorido.meeting.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompleteMeetingRequest {
  private List<String> actualAttendees;
  private String notes;
}
