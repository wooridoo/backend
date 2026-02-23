package com.woorido.vote.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.AuthHeaderResolver;
import com.woorido.vote.dto.VoteDto;
import com.woorido.vote.dto.request.CastVoteRequest;
import com.woorido.vote.dto.request.CreateVoteRequest;
import com.woorido.vote.dto.response.CastVoteResponse;
import com.woorido.vote.dto.response.VoteListResponse;
import com.woorido.vote.dto.response.VoteResultResponse;
import com.woorido.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VoteController {
  // Learning note:
  // - Controller parses request/header and delegates business rules to Service.
  // - Keep API response mapping here, keep domain rules in Service.

  private final VoteService voteService;
  private final AuthHeaderResolver authHeaderResolver;

  @GetMapping("/challenges/{challengeId}/votes")
  public ResponseEntity<ApiResponse<VoteListResponse>> getVoteList(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "type", required = false) String type,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {

    try {
      String userId = extractUserId(authorization);
      VoteListResponse response = voteService.getVoteList(challengeId, userId, status, type, page, size);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/votes/{voteId}")
  public ResponseEntity<ApiResponse<Object>> getVoteDetail(
      @PathVariable("voteId") String voteId,
      @RequestHeader("Authorization") String authorization) {

    try {
      String userId = extractUserId(authorization);
      Object response = voteService.getVoteDetail(voteId, userId);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PostMapping("/challenges/{challengeId}/votes")
  public ResponseEntity<ApiResponse<VoteDto>> createVote(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody CreateVoteRequest request) {

    try {
      String userId = extractUserId(authorization);
      VoteDto response = voteService.createVote(challengeId, userId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/votes/{voteId}/cast")
  public ResponseEntity<ApiResponse<CastVoteResponse>> castVote(
      @PathVariable("voteId") String voteId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody CastVoteRequest request) {

    try {
      String userId = extractUserId(authorization);
      CastVoteResponse response = voteService.castVote(voteId, userId, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/votes/{voteId}/result")
  public ResponseEntity<ApiResponse<VoteResultResponse>> getVoteResult(
      @PathVariable("voteId") String voteId,
      @RequestHeader("Authorization") String authorization) {

    try {
      String userId = extractUserId(authorization);
      VoteResultResponse response = voteService.getVoteResult(voteId, userId);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  private String extractUserId(String authorization) {
    return authHeaderResolver.resolveAccessUserId(authorization);
  }

  private <T> ResponseEntity<ApiResponse<T>> handleError(RuntimeException e) {
    String message = e.getMessage() != null ? e.getMessage() : "서버 오류가 발생했습니다";

    if (message.startsWith("AUTH_")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }
    if (message.startsWith("CHALLENGE_003") || message.startsWith("VOTE_003") || message.startsWith("VOTE_008")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }
    if (message.startsWith("CHALLENGE_001") || message.startsWith("VOTE_001") || message.startsWith("MEETING_001")) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }
    if (message.startsWith("VOTE_006")) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }
    if (message.startsWith("VOTE_") || message.startsWith("MEETING_")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("서버 오류가 발생했습니다"));
  }
}
