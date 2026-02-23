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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.challenge.dto.request.ChallengeListRequest;
import com.woorido.challenge.dto.request.CreateChallengeRequest;
import com.woorido.challenge.dto.request.MyChallengesRequest;
import com.woorido.challenge.dto.request.UpdateChallengeRequest;
import com.woorido.challenge.dto.response.ChallengeAccountResponse;
import com.woorido.challenge.dto.response.ChallengeDetailResponse;
import com.woorido.challenge.dto.response.ChallengeLedgerGraphResponse;
import com.woorido.challenge.dto.response.ChallengeListResponse;
import com.woorido.challenge.dto.response.CreateChallengeResponse;
import com.woorido.challenge.dto.response.ChallengeDeleteResponse;
import com.woorido.challenge.dto.request.UpdateSupportSettingsRequest;
import com.woorido.challenge.dto.response.UpdateSupportSettingsResponse;
import com.woorido.challenge.dto.response.ChallengeMemberListResponse;
import com.woorido.challenge.dto.response.JoinChallengeResponse;
import com.woorido.challenge.dto.response.LeaveChallengeResponse;
import com.woorido.challenge.dto.response.MyChallengesResponse;
import com.woorido.challenge.dto.response.UpdateChallengeResponse;
import com.woorido.challenge.dto.request.DelegateLeaderRequest;
import com.woorido.challenge.dto.response.DelegateLeaderResponse;
import com.woorido.challenge.service.ChallengeService;
import com.woorido.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
@Slf4j
public class ChallengeController {
  // Learning note:
  // - Controller parses request/header and delegates business rules to Service.
  // - Keep API response mapping here, keep domain rules in Service.

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
      log.error(
          "챌린지 목록 조회 실패: status={}, category={}, sort={}, page={}, size={}",
          request.getStatus(),
          request.getCategory(),
          request.getSort(),
          request.getPage(),
          request.getSize(),
          e);

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
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("인증에 실패했습니다"));
    } catch (Exception e) {
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
      log.error("Challenge API error: {}", e.getMessage(), e);

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
   * 챌린지 계좌 조회 API (API 028)
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
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("챌린지 멤버가 아닙니다"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 챌린지 계좌 그래프 조회 API
   * GET /challenges/{challengeId}/account/graph
   */
  @GetMapping("/{challengeId}/account/graph")
  public ResponseEntity<ApiResponse<ChallengeLedgerGraphResponse>> getChallengeAccountGraph(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "months", required = false) Integer months) {

    try {
      ChallengeLedgerGraphResponse response = challengeService.getChallengeLedgerGraph(challengeId, authorization, months);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("챌린지 멤버가 아닙니다"));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("LEDGER_004")) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(message));
      }
      throw e;
    } catch (Exception e) {
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
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
    } catch (IllegalStateException e) {
      String message = e.getMessage();
      if ("CHALLENGE_002".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("CHALLENGE_002:Already joined challenge"));
      if ("CHALLENGE_005".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("CHALLENGE_005:Challenge member limit exceeded"));
      if ("CHALLENGE_006".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("모집 중인 챌린지가 아닙니다"));
      if ("ACCOUNT_004".equals(message))
        return ResponseEntity.badRequest().body(ApiResponse.error("잔액이 부족합니다"));
      return ResponseEntity.badRequest().body(ApiResponse.error(message));
    } catch (Exception e) {
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
      log.error("Challenge API error: {}", e.getMessage(), e);

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
        } else if (message.startsWith("CHALLENGE_011")) {
          return ResponseEntity.status(HttpStatus.CONFLICT)
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
      log.error("Challenge API error: {}", e.getMessage(), e);

      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_001")) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("CHALLENGE_007")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.error(message));
        } else if (message.startsWith("CHALLENGE_011")) {
          return ResponseEntity.status(HttpStatus.CONFLICT)
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
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
        if (message.startsWith("MEMBER_002"))
          return ResponseEntity.badRequest().body(ApiResponse.error("리더는 챌린지를 탈퇴할 수 없습니다"));
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
      @RequestHeader("Authorization") String authorization,
      @org.springframework.web.bind.annotation.RequestParam(value = "status", required = false) String status) {

    try {
      ChallengeMemberListResponse response = challengeService.getChallengeMembers(challengeId, authorization, status);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
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

  /**
   * 챌린지 해산 API (API 026)
   * DELETE /challenges/{challengeId}
   */
  @DeleteMapping("/{challengeId}")
  public ResponseEntity<ApiResponse<ChallengeDeleteResponse>> deleteChallenge(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization) {

    try {
      ChallengeDeleteResponse response = challengeService
          .deleteChallenge(authorization, challengeId);
      return ResponseEntity.ok(ApiResponse.success(response, "Challenge deleted successfully"));
    } catch (RuntimeException e) {
      log.error("Challenge API error: {}", e.getMessage(), e);

      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_004"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("리더만 삭제할 수 있습니다"));
        if (message.startsWith("CHALLENGE_010"))
          return ResponseEntity.badRequest().body(ApiResponse.error("모집 중 상태의 챌린지만 삭제할 수 있습니다"));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 자동 후원 설정 API (API 029)
   * PUT /challenges/{challengeId}/support/settings
   */
  @PutMapping("/{challengeId}/support/settings")
  public ResponseEntity<ApiResponse<UpdateSupportSettingsResponse>> updateSupportSettings(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody UpdateSupportSettingsRequest request) {

    try {
      UpdateSupportSettingsResponse response = challengeService
          .updateSupportSettings(challengeId, authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response, "Support settings updated successfully"));
    } catch (RuntimeException e) {
      log.error("Challenge API error: {}", e.getMessage(), e);

      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_001"))
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
        if (message.startsWith("CHALLENGE_003"))
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
        if (message.startsWith("VALIDATION_"))
          return ResponseEntity.badRequest().body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * API 033: 챌린지 멤버 상세 조회
   * GET /challenges/{challengeId}/members/{memberId}
   */
  @GetMapping("/{challengeId}/members/{memberId}")
  public ResponseEntity<ApiResponse<com.woorido.challenge.dto.response.ChallengeMemberDetailResponse>> getMemberDetail(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("memberId") String memberId,
      @RequestHeader("Authorization") String authorization) {

    try {
      com.woorido.challenge.dto.response.ChallengeMemberDetailResponse response = challengeService
          .getMemberDetail(challengeId, memberId, authorization);
      return ResponseEntity.ok(ApiResponse.success(response, "멤버 상세 정보 조회 성공"));
    } catch (RuntimeException e) {
      log.error("API 033 Error: {}", e.getMessage(), e);
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_") || message.startsWith("MEMBER_")) {
          // 403 or 404 depending on error, but for simplicity returning 400 or specific
          // status
          if (message.startsWith("CHALLENGE_003"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("챌린지 멤버가 아닙니다"));
          if (message.startsWith("MEMBER_001"))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("멤버를 찾을 수 없습니다"));
          return ResponseEntity.badRequest().body(ApiResponse.error(message));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  // API 034: 리더 위임
  @PostMapping("/{challengeId}/delegate")
  public ResponseEntity<ApiResponse<DelegateLeaderResponse>> delegateLeader(
      @RequestHeader("Authorization") String authorization,
      @PathVariable("challengeId") String challengeId,
      @RequestBody DelegateLeaderRequest request) {

    try {
      String token = authorization;
      if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
      }

      String targetId = request.getTargetUserId();
      if (targetId == null) {
        targetId = request.getTargetMemberId();
      }

      DelegateLeaderResponse response = challengeService.delegateLeaderWithToken(challengeId, token, targetId);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_")) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        }
        if (message.startsWith("CHALLENGE_001")) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
        }
        if (message.startsWith("CHALLENGE_003") || message.startsWith("CHALLENGE_004")) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
        }
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(message != null ? message : "Invalid request"));
    }
  }
}
