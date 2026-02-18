package com.woorido.meeting.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    try {
      MeetingListResponse response = meetingService.getMeetingList(challengeId, authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001"))
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        if (message.startsWith("CHALLENGE_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 모임 상세 조회 API (API 036)
   * GET /meetings/{meetingId}
   */
  @GetMapping("/meetings/{meetingId}")
  public ResponseEntity<ApiResponse<Object>> getMeetingDetail(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization) {

    try {
      Object response = meetingService.getMeetingDetail(meetingId, authorization);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001"))
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        if (message.startsWith("MEETING_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("모임을 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
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

    try {
      Object response = meetingService.createMeeting(challengeId, authorization, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001"))
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
        if (message.startsWith("CHALLENGE_004"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("리더만 모임을 생성할 수 있습니다"));
        if (message.startsWith("MEETING_004"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("예정 일시는 최소 24시간 이후여야 합니다"));
        if (message.startsWith("CHALLENGE_005"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("활성 멤버가 없습니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 모임 수정 API (API 038)
   * PUT /meetings/{meetingId}
   */
  @org.springframework.web.bind.annotation.PutMapping("/meetings/{meetingId}")
  public ResponseEntity<ApiResponse<Object>> updateMeeting(
      @PathVariable("meetingId") String meetingId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody com.woorido.meeting.dto.request.UpdateMeetingRequest request) {

    try {
      Object response = meetingService.updateMeeting(meetingId, authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001"))
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        if (message.startsWith("MEETING_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("모임을 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_004"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("리더만 모임을 수정할 수 있습니다"));
        if (message.startsWith("MEETING_002"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("이미 지난 모임은 수정할 수 없습니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
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

    try {
      Object response = meetingService.respondAttendance(meetingId, authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001"))
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        if (message.startsWith("MEETING_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("모임을 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
        if (message.startsWith("MEETING_002"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("이미 지난 모임입니다"));
        if (message.startsWith("MEETING_003"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("이미 참석 의사를 표시했습니다"));
        if (message.startsWith("MEETING_006"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("모임 참석 투표가 종료되었습니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
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

    try {
      Object response = meetingService.completeMeeting(meetingId, authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001"))
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        if (message.startsWith("MEETING_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("모임을 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_004"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("리더만 완료 처리할 수 있습니다"));
        if (message.startsWith("MEETING_005"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("이미 완료된 모임입니다"));
        if (message.startsWith("ACCOUNT_004"))
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("챌린지 잔액이 부족하거나 계좌 오류입니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }
}
