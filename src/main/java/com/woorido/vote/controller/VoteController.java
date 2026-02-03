package com.woorido.vote.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import com.woorido.vote.dto.response.VoteListResponse;
import com.woorido.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class VoteController {

  private final VoteService voteService;
  private final JwtUtil jwtUtil;

  /**
   * API 041: 투표 목록 조회
   * GET /challenges/{challengeId}/votes
   */
  @GetMapping("/challenges/{challengeId}/votes")
  public ResponseEntity<ApiResponse<VoteListResponse>> getVoteList(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "type", required = false) String type,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {

    try {
      String token = authorization.replace("Bearer ", "");
      String userId = jwtUtil.getUserIdFromToken(token);

      VoteListResponse response = voteService.getVoteList(challengeId, userId, status, type, page, size);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_003")) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(ApiResponse.error("챌린지 멤버가 아닙니다"));
        } else if (message.startsWith("CHALLENGE_001")) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(ApiResponse.error("챌린지를 찾을 수 없습니다"));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + message));
    }
  }

  /**
   * API 042: 투표 상세 조회
   * GET /votes/{voteId}
   */
  @GetMapping("/votes/{voteId}")
  public ResponseEntity<ApiResponse<Object>> getVoteDetail(
      @PathVariable("voteId") String voteId,
      @RequestHeader("Authorization") String authorization) {

    try {
      String token = authorization.replace("Bearer ", "");
      String userId = jwtUtil.getUserIdFromToken(token);

      Object response = voteService.getVoteDetail(voteId, userId);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("VOTE_003")) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(ApiResponse.error("투표 조회 권한이 없습니다"));
        } else if (message.startsWith("VOTE_001")) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(ApiResponse.error("투표를 찾을 수 없습니다"));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + message));
    }
  }

  /**
   * API 043: 투표 생성
   * POST /challenges/{challengeId}/votes
   */
  @PostMapping("/challenges/{challengeId}/votes")
  public ResponseEntity<ApiResponse<com.woorido.vote.dto.VoteDto>> createVote(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody com.woorido.vote.dto.request.CreateVoteRequest request) {

    try {
      String token = authorization.replace("Bearer ", "");
      String userId = jwtUtil.getUserIdFromToken(token);

      com.woorido.vote.dto.VoteDto response = voteService.createVote(challengeId, userId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("CHALLENGE_003")) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(ApiResponse.error("챌린지 멤버가 아닙니다"));
        } else if (message.startsWith("VOTE_002")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.error("마감 시간은 최소 24시간 이후여야 합니다"));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + message));
    }
  }

  /**
   * API 044: 투표하기
   * PUT /votes/{voteId}/cast
   */
  @PutMapping("/votes/{voteId}/cast")
  public ResponseEntity<ApiResponse<com.woorido.vote.dto.response.CastVoteResponse>> castVote(
      @PathVariable("voteId") String voteId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody com.woorido.vote.dto.request.CastVoteRequest request) {

    try {
      String token = authorization.replace("Bearer ", "");
      String userId = jwtUtil.getUserIdFromToken(token);

      com.woorido.vote.dto.response.CastVoteResponse response = voteService.castVote(voteId, userId, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("VOTE_003")) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("투표 권한이 없습니다"));
        } else if (message.startsWith("VOTE_001")) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("투표를 찾을 수 없습니다"));
        } else if (message.startsWith("VOTE_005")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("투표가 이미 마감되었습니다"));
        } else if (message.startsWith("VOTE_006")) {
          return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("이미 투표하셨습니다"));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + message));
    }
  }
}
