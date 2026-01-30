package com.woorido.challenge.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.challenge.dto.request.ChallengeListRequest;
import com.woorido.challenge.dto.request.CreateChallengeRequest;
import com.woorido.challenge.dto.request.MyChallengesRequest;
import com.woorido.challenge.dto.request.UpdateChallengeRequest;
import com.woorido.challenge.dto.response.ChallengeAccountResponse;
import com.woorido.challenge.dto.response.ChallengeDetailResponse;
import com.woorido.challenge.dto.response.ChallengeListResponse;
import com.woorido.challenge.dto.response.CreateChallengeResponse;
import com.woorido.challenge.dto.response.ChallengeMemberListResponse;
import com.woorido.challenge.dto.response.JoinChallengeResponse;
import com.woorido.challenge.dto.response.LeaveChallengeResponse;
import com.woorido.challenge.dto.response.MyChallengesResponse;
import com.woorido.challenge.dto.response.UpdateChallengeResponse;
import com.woorido.challenge.service.ChallengeService;
import com.woorido.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
public class ChallengeController {

  private final ChallengeService challengeService;

  /**
   * 챌린지 목록 조회 API (API 023)
   * GET /challenges
   */
  @GetMapping
  public ResponseEntity<ApiResponse<ChallengeListResponse>> getChallengeList(
      @ModelAttribute ChallengeListRequest request) {

    try {
      ChallengeListResponse response = challengeService.getChallengeList(request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 내 챌린지 목록 조회 API (API 027)
   * GET /challenges/me
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<MyChallengesResponse>> getMyChallenges(
      @RequestHeader("Authorization") String authorization,
      @ModelAttribute MyChallengesRequest request) {

    try {
      MyChallengesResponse response = challengeService.getMyChallenges(authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("인증에 실패했습니다"));
    } catch (Exception e) {
      System.out.println("에러 발생: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 상세 조회 API (API 024)
   * GET /challenges/{challengeId}
   */
  @GetMapping("/{challengeId}")
  public ResponseEntity<ApiResponse<ChallengeDetailResponse>> getChallengeDetail(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {

    try {
      ChallengeDetailResponse response = challengeService.getChallengeDetail(challengeId, authorization);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());
      e.printStackTrace();

      String message = e.getMessage();
      if (message != null && message.startsWith("CHALLENGE_001")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 어카운트 조회 API (API 028)
   * GET /challenges/{challengeId}/account
   */
  @GetMapping("/{challengeId}/account")
  public ResponseEntity<ApiResponse<ChallengeAccountResponse>> getChallengeAccount(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization) {

    try {
      ChallengeAccountResponse response = challengeService.getChallengeAccount(challengeId, authorization);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (IllegalArgumentException e) {
      System.out.println("에러 발생: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
    } catch (SecurityException e) {
      System.out.println("에러 발생: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("챌린지 멤버가 아닙니다"));
    } catch (Exception e) {
      System.out.println("에러 발생: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 가입 API (API 030)
   * POST /challenges/{challengeId}/join
   */
  @PostMapping("/{challengeId}/join")
  public ResponseEntity<ApiResponse<JoinChallengeResponse>> joinChallenge(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization) {

    try {
      JoinChallengeResponse response = challengeService.joinChallenge(challengeId, authorization);
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    } catch (IllegalArgumentException e) {
      System.out.println("에러 발생: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
    } catch (IllegalStateException e) {
      System.out.println("에러 발생: " + e.getMessage());
      String message = e.getMessage();
      if ("CHALLENGE_002".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("이미 가입한 챌린지입니다"));
      if ("CHALLENGE_005".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("챌린지 정원이 초과되었습니다"));
      if ("CHALLENGE_006".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("모집 중인 챌린지가 아닙니다"));
      if ("ACCOUNT_004".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("잔액이 부족합니다"));
      return ResponseEntity.badRequest().body(ApiResponse.error(message));
    } catch (Exception e) {
      System.out.println("에러 발생: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 수정 API (API 025)
   * PUT /challenges/{challengeId}
   */
  @PutMapping("/{challengeId}")
  public ResponseEntity<ApiResponse<UpdateChallengeResponse>> updateChallenge(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody UpdateChallengeRequest request) {

    try {
      UpdateChallengeResponse response = challengeService.updateChallenge(challengeId, authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());
      e.printStackTrace();

      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001")) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("CHALLENGE_001")) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("CHALLENGE_004")) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("VALIDATION_001")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.error(message));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 생성 API (API 022)
   * POST /challenges
   */
  @PostMapping
  public ResponseEntity<ApiResponse<CreateChallengeResponse>> createChallenge(
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody CreateChallengeRequest request) {

    try {
      CreateChallengeResponse response = challengeService.createChallenge(authorization, request);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, response.getMessage()));
    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());
      e.printStackTrace();

      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001")) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("CHALLENGE_007")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("VALIDATION_001")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("ACCOUNT_")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.error(message));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 탈퇴 API (API 031)
   * DELETE /challenges/{challengeId}/leave
   */
  @DeleteMapping("/{challengeId}/leave")
  public ResponseEntity<ApiResponse<LeaveChallengeResponse>> leaveChallenge(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization) {

    try {
      LeaveChallengeResponse response = challengeService.leaveChallenge(challengeId, authorization);
      return ResponseEntity.ok(ApiResponse.success(response, "챌린지에서 탈퇴했습니다"));
    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());

      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
        if (message.startsWith("MEMBER_002"))
          return ResponseEntity.badRequest().body(ApiResponse.error("리더는 탈퇴할 수 없습니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 멤버 목록 조회 API (API 032)
   * GET /challenges/{challengeId}/members
   */
  @GetMapping("/{challengeId}/members")
  public ResponseEntity<ApiResponse<ChallengeMemberListResponse>> getChallengeMembers(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization) {

    try {
      ChallengeMemberListResponse response = challengeService.getChallengeMembers(challengeId, authorization);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());

      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }
}
