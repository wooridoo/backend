package com.woorido.meeting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.common.dto.ApiResponse;
import com.woorido.meeting.dto.request.CreateMeetingRequest;
import com.woorido.meeting.dto.request.MeetingListRequest;
import com.woorido.meeting.dto.request.UpdateMeetingRequest;
import com.woorido.meeting.dto.response.MeetingListResponse;
import com.woorido.meeting.service.MeetingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MeetingController {

  private final MeetingService meetingService;

  /**
   * 모임 목록 조회 API (API 035)
   * GET /challenges/{challengeId}/meetings
   */
  @GetMapping("/challenges/{challengeId}/meetings")
  public ResponseEntity<ApiResponse<MeetingListResponse>> getMeetingList(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @ModelAttribute MeetingListRequest request) {
    MeetingListResponse response = meetingService.getMeetingList(challengeId, authorization, request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 모임 상세 조회 API (API 036)
   * GET /meetings/{meetingId}
   */
  @GetMapping("/meetings/{meetingId}")
  public ResponseEntity<ApiResponse<Object>> getMeetingDetail(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization) {
    Object response = meetingService.getMeetingDetail(meetingId, authorization);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 모임 생성 API (API 037)
   * POST /challenges/{challengeId}/meetings
   */
  @PostMapping("/challenges/{challengeId}/meetings")
  public ResponseEntity<ApiResponse<Object>> createMeeting(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody CreateMeetingRequest request) {
    Object response = meetingService.createMeeting(challengeId, authorization, request);
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
        .body(ApiResponse.success(response));
  }

  /**
   * 모임 수정 API (API 038)
   * PUT /meetings/{meetingId}
   */
  @org.springframework.web.bind.annotation.PutMapping("/meetings/{meetingId}")
  public ResponseEntity<ApiResponse<Object>> updateMeeting(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody UpdateMeetingRequest request) {
    Object response = meetingService.updateMeeting(meetingId, authorization, request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 참석 의사 표시 API (API 039)
   * POST /meetings/{meetingId}/attendance
   */
  @PostMapping("/meetings/{meetingId}/attendance")
  public ResponseEntity<ApiResponse<Object>> respondAttendance(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody com.woorido.meeting.dto.request.AttendanceResponseRequest request) {
    Object response = meetingService.respondAttendance(meetingId, authorization, request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 모임 완료 처리 API (API 040)
   * POST /meetings/{meetingId}/complete
   */
  @PostMapping("/meetings/{meetingId}/complete")
  public ResponseEntity<ApiResponse<Object>> completeMeeting(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody com.woorido.meeting.dto.request.CompleteMeetingRequest request) {
    Object response = meetingService.completeMeeting(meetingId, authorization, request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 모임 삭제 API (API 041)
   * DELETE /meetings/{meetingId}
   */
  @DeleteMapping("/meetings/{meetingId}")
  public ResponseEntity<ApiResponse<Object>> deleteMeeting(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization) {
    Object response = meetingService.deleteMeeting(meetingId, authorization);
    return ResponseEntity.ok(ApiResponse.success(response, "모임이 삭제되었습니다"));
  }

  /**
   * 참석 취소 API (API 092)
   * DELETE /meetings/{meetingId}/attendance
   */
  @DeleteMapping("/meetings/{meetingId}/attendance")
  public ResponseEntity<ApiResponse<Object>> cancelAttendance(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization) {
    Object response = meetingService.cancelAttendance(meetingId, authorization);
    return ResponseEntity.ok(ApiResponse.success(response, "참석 의사가 취소되었습니다"));
  }
}
