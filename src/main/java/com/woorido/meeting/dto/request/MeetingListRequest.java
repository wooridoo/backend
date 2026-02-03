package com.woorido.meeting.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingListRequest {
  private String status; // N, SCHEDULED, COMPLETED, CANCELED
  private Integer page = 0;
  private Integer size = 20;
}
