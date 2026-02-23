package com.woorido.post.service;

import com.woorido.common.exception.RateLimitExceededException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SocialRateLimitService {
  private static final long WINDOW_MILLIS = 60_000L;
  private static final int COMMENT_WRITE_PER_MINUTE = 20;
  private static final int LIKE_WRITE_PER_MINUTE = 60;
  private static final int SNS_WRITE_PER_IP_PER_MINUTE = 120;

  private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

  /**
   * 댓글/대댓글 생성 레이트리밋.
   */
  public void checkCommentWriteLimit(String userId, String clientIp) {
    long retryAfterByUser = registerAndGetRetryAfterSeconds(
        "sns:comment:user:" + normalize(userId),
        COMMENT_WRITE_PER_MINUTE,
        WINDOW_MILLIS);
    if (retryAfterByUser > 0) {
      throw new RateLimitExceededException("ABUSE_001:SNS write rate limit 초과", retryAfterByUser);
    }

    long retryAfterByIp = registerAndGetRetryAfterSeconds(
        "sns:write:ip:" + normalize(clientIp),
        SNS_WRITE_PER_IP_PER_MINUTE,
        WINDOW_MILLIS);
    if (retryAfterByIp > 0) {
      throw new RateLimitExceededException("ABUSE_001:SNS write rate limit 초과", retryAfterByIp);
    }
  }

  /**
   * 좋아요 토글(게시글/댓글) 레이트리밋.
   */
  public void checkLikeWriteLimit(String userId, String clientIp) {
    long retryAfterByUser = registerAndGetRetryAfterSeconds(
        "sns:like:user:" + normalize(userId),
        LIKE_WRITE_PER_MINUTE,
        WINDOW_MILLIS);
    if (retryAfterByUser > 0) {
      throw new RateLimitExceededException("ABUSE_001:SNS write rate limit 초과", retryAfterByUser);
    }

    long retryAfterByIp = registerAndGetRetryAfterSeconds(
        "sns:write:ip:" + normalize(clientIp),
        SNS_WRITE_PER_IP_PER_MINUTE,
        WINDOW_MILLIS);
    if (retryAfterByIp > 0) {
      throw new RateLimitExceededException("ABUSE_001:SNS write rate limit 초과", retryAfterByIp);
    }
  }

  /**
   * Sliding window 방식 카운팅.
   * 제한을 넘기면 재시도까지 남은 초를 반환한다.
   */
  private long registerAndGetRetryAfterSeconds(String key, int limit, long windowMillis) {
    Deque<Long> bucket = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    long now = System.currentTimeMillis();

    synchronized (bucket) {
      long threshold = now - windowMillis;
      while (!bucket.isEmpty() && bucket.peekFirst() <= threshold) {
        bucket.pollFirst();
      }

      if (bucket.size() >= limit) {
        Long oldest = bucket.peekFirst();
        if (oldest == null) {
          return 1L;
        }
        long retryAfterMillis = windowMillis - (now - oldest);
        return Math.max(1L, (long) Math.ceil(retryAfterMillis / 1000.0));
      }

      bucket.addLast(now);
      return 0L;
    }
  }

  private String normalize(String value) {
    if (value == null) {
      return "unknown";
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? "unknown" : trimmed;
  }
}
