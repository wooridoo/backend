package com.woorido.post.service;

import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.common.dto.AuthorInfo;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.notification.domain.NotificationType;
import com.woorido.notification.service.NotificationService;
import com.woorido.post.domain.Comment;
import com.woorido.post.domain.CommentDeleteStrategy;
import com.woorido.post.domain.CommentLike;
import com.woorido.post.domain.CommentUpdateVisitor;
import com.woorido.post.domain.Post;
import com.woorido.post.dto.request.CreateCommentRequest;
import com.woorido.post.dto.request.UpdateCommentRequest;
import com.woorido.post.dto.response.CommentResponse;
import com.woorido.post.dto.response.DeleteCommentResponse;
import com.woorido.post.dto.response.UpdateCommentResponse;
import com.woorido.post.repository.CommentLikeMapper;
import com.woorido.post.repository.CommentMapper;
import com.woorido.post.repository.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
  // 학습 포인트:
  // - 댓글/좋아요는 항상 challengeId, postId 경계를 함께 검증해 데이터 오염을 막는다.

  private final CommentMapper commentMapper;
  private final UserMapper userMapper;
  private final CommentLikeMapper commentLikeMapper;
  private final CommentDeleteStrategy commentDeleteStrategy;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final PostMapper postMapper;
  private final SocialRateLimitService socialRateLimitService;
  private final NotificationService notificationService;
  private final Map<String, Long> duplicateCommentWriteCache = new ConcurrentHashMap<>();

  private static final int MAX_COMMENT_DEPTH = 50;
  private static final long DUPLICATE_COMMENT_WINDOW_MILLIS = 15_000L;

  /**
   * 댓글 생성.
   * 흐름: 멤버/게시글 검증 -> (대댓글인 경우) 부모 댓글 검증 -> 댓글 저장
   */
  @Transactional
  public String createComment(
      String challengeId,
      String postId,
      String userId,
      String clientIp,
      CreateCommentRequest request) {
    // 요청자가 해당 챌린지의 유효 멤버인지 확인
    requireActiveMember(challengeId, userId);
    // 댓글 대상 게시글이 현재 챌린지에 속하는지 확인
    Post post = requirePostInChallenge(challengeId, postId);
    socialRateLimitService.checkCommentWriteLimit(userId, clientIp);

    String content = request.getContent() == null ? "" : request.getContent().trim();
    if (content.isEmpty()) {
      throw new IllegalArgumentException("VALIDATION_001:댓글 내용을 입력해주세요");
    }

    String normalizedContent = normalizeContent(content);
    guardAgainstDuplicateCommentWrite(postId, userId, normalizedContent);

    String parentCommentId = request.getParentId();
    if (parentCommentId != null && parentCommentId.isBlank()) {
      parentCommentId = null;
    }

    Comment parentComment = null;
    if (parentCommentId != null) {
      // 대댓글이면 부모 댓글 존재 여부/게시글 일치 여부를 확인
      parentComment = commentMapper.findById(parentCommentId)
          .orElseThrow(() -> new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다"));
      if (!postId.equals(parentComment.getPostId())) {
        throw new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다");
      }

      int parentDepth = computeDepthFromRoot(parentComment);
      if (parentDepth >= MAX_COMMENT_DEPTH) {
        throw new IllegalArgumentException("COMMENT_004:허용 depth를 초과했습니다");
      }
    }

    String id = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();

    Comment comment = Comment.builder()
        .id(id)
        .postId(postId)
        .parentId(parentCommentId)
        .createdBy(userId)
        .content(content)
        .createdAt(now)
        .updatedAt(now)
        .build();

    commentMapper.save(comment);
    postMapper.increaseCommentCount(postId);

    String actorName = resolveActorName(userId);
    String linkUrl = buildFeedLink(challengeId, postId);

    if (parentComment != null) {
      notificationService.publishSocialNotification(
          NotificationType.COMMENT_REPLIED,
          parentComment.getCreatedBy(),
          userId,
          parentComment.getId(),
          "새 답글",
          actorName + "님이 회원님의 댓글에 답글을 남겼습니다.",
          "회원님의 댓글에 새 답글이 도착했습니다.",
          linkUrl,
          "COMMENT",
          parentComment.getId());
    } else {
      notificationService.publishSocialNotification(
          NotificationType.POST_COMMENTED,
          post.getCreatedBy(),
          userId,
          postId,
          "새 댓글",
          actorName + "님이 회원님의 게시글에 댓글을 남겼습니다.",
          "회원님의 게시글에 새 댓글이 도착했습니다.",
          linkUrl,
          "POST",
          postId);
    }

    return comment.getId();
  }

  /**
   * 댓글 좋아요 토글.
   * 이미 좋아요가 있으면 취소, 없으면 생성한다.
   */
  @Transactional
  public boolean toggleLike(String challengeId, String postId, String commentId, String userId, String clientIp) {
    socialRateLimitService.checkLikeWriteLimit(userId, clientIp);
    // 경계/권한 검증
    requireActiveMember(challengeId, userId);
    requirePostInChallenge(challengeId, postId);
    Comment targetComment = requireCommentInPost(commentId, postId);

    if (commentLikeMapper.exists(commentId, userId)) {
      commentLikeMapper.delete(commentId, userId);
      commentMapper.decreaseLikeCount(commentId);
      return false;
    }

    commentLikeMapper.save(CommentLike.builder()
        .id(UUID.randomUUID().toString())
        .commentId(commentId)
        .userId(userId)
        .createdAt(LocalDateTime.now())
        .build());
    commentMapper.increaseLikeCount(commentId);

    String actorName = resolveActorName(userId);
    String linkUrl = buildFeedLink(challengeId, postId);
    notificationService.publishSocialNotification(
        NotificationType.COMMENT_LIKED,
        targetComment.getCreatedBy(),
        userId,
        commentId,
        "댓글 좋아요",
        actorName + "님이 회원님의 댓글을 좋아합니다.",
        "회원님의 댓글에 좋아요가 도착했습니다.",
        linkUrl,
        "COMMENT",
        commentId);

    return true;
  }

  /**
   * 게시글 댓글 목록 조회.
   * 루트 댓글을 기준으로 답글 트리를 재귀적으로 구성한다.
   */
  @Transactional(readOnly = true)
  public List<CommentResponse> getComments(String challengeId, String postId, String userId, int page, int size) {
    requireReadableMember(challengeId, userId);
    requirePostInChallenge(challengeId, postId);

    List<Comment> comments = commentMapper.findAllByPostId(postId);
    if (comments.isEmpty()) {
      return new ArrayList<>();
    }

    Set<String> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toSet());
    Set<String> likedCommentIds = resolveLikedCommentIds(commentIds, userId);

    List<String> userIds = comments.stream()
        .map(Comment::getCreatedBy)
        .distinct()
        .collect(Collectors.toList());

    Map<String, AuthorInfo> authorMap = new HashMap<>();
    for (String authorUserId : userIds) {
      // 현재는 사용자 정보를 개별 조회(N+1)한다.
      // 트래픽이 커지면 IN 조회 방식으로 최적화할 수 있다.
      User user = userMapper.findById(authorUserId);
      if (user != null) {
        authorMap.put(authorUserId, AuthorInfo.builder()
            .userId(user.getId())
            .nickname(user.getNickname())
            .profileImage(user.getProfileImageUrl())
            .build());
      }
    }

    List<CommentResponse> rootComments = comments.stream()
        .filter(c -> c.getParentId() == null || !commentIds.contains(c.getParentId()))
        .map(c -> toCommentResponse(c, comments, authorMap, likedCommentIds))
        .collect(Collectors.toList());

    int safePage = Math.max(page, 0);
    int safeSize = Math.max(size, 1);
    int fromIndex = Math.min(safePage * safeSize, rootComments.size());
    int toIndex = Math.min(fromIndex + safeSize, rootComments.size());
    return rootComments.subList(fromIndex, toIndex);
  }

  /**
   * 댓글 수정.
   * 작성자 본인만 수정 가능하며 Visitor로 변경값을 반영한다.
   */
  public UpdateCommentResponse updateComment(String challengeId, String postId,
      String commentId, String userId, UpdateCommentRequest request) {

    requireActiveMember(challengeId, userId);
    requirePostInChallenge(challengeId, postId);

    Comment comment = requireCommentInPost(commentId, postId);
    if (!userId.equals(comment.getCreatedBy())) {
      throw new IllegalArgumentException("COMMENT_003: 수정 권한이 없습니다");
    }

    CommentUpdateVisitor visitor = new CommentUpdateVisitor(request);
    comment.accept(visitor);
    commentMapper.update(comment);

    return UpdateCommentResponse.builder()
        .commentId(comment.getId())
        .content(comment.getContent())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }

  /**
   * 댓글 삭제.
   * 자식 댓글이 있으면 소프트 삭제, 없으면 물리 삭제를 적용한다.
   */
  public DeleteCommentResponse deleteComment(String challengeId, String postId, String commentId, String userId) {
    Map<String, Object> memberInfo = requireActiveMember(challengeId, userId);
    requirePostInChallenge(challengeId, postId);

    Comment comment = requireCommentInPost(commentId, postId);
    String role = memberInfo != null ? asString(memberInfo.get("ROLE")) : null;

    commentDeleteStrategy.validate(comment, userId, role);

    int childCount = commentMapper.countByParentId(commentId);
    if (childCount > 0) {
      comment.accept(new com.woorido.post.domain.CommentDeleteVisitor());
      commentMapper.update(comment);
    } else {
      commentLikeMapper.deleteByCommentId(commentId);
      commentMapper.deletePhysical(commentId);
      postMapper.decreaseCommentCount(postId);
    }

    return DeleteCommentResponse.builder()
        .commentId(commentId)
        .deletedAt(LocalDateTime.now())
        .build();
  }

  private List<CommentResponse> getReplies(
      String parentId,
      List<Comment> allComments,
      Map<String, AuthorInfo> authorMap,
      Set<String> likedCommentIds) {
    return allComments.stream()
        .filter(c -> parentId.equals(c.getParentId()))
        .map(c -> toCommentResponse(c, allComments, authorMap, likedCommentIds))
        .collect(Collectors.toList());
  }

  private CommentResponse toCommentResponse(
      Comment comment,
      List<Comment> allComments,
      Map<String, AuthorInfo> authorMap,
      Set<String> likedCommentIds) {
    return CommentResponse.builder()
        .id(comment.getId())
        .commentId(comment.getId())
        .content(comment.getContent())
        .author(authorMap.get(comment.getCreatedBy()))
        .likeCount(comment.getLikeCount())
        .isDeleted(comment.getDeletedAt() != null)
        .isLiked(likedCommentIds.contains(comment.getId()))
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .parentId(comment.getParentId())
        .replies(getReplies(comment.getId(), allComments, authorMap, likedCommentIds))
        .build();
  }

  private Set<String> resolveLikedCommentIds(Set<String> commentIds, String userId) {
    if (commentIds.isEmpty()) {
      return Set.of();
    }
    List<String> likedIds = commentLikeMapper.findLikedCommentIds(new ArrayList<>(commentIds), userId);
    if (likedIds == null || likedIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(likedIds);
  }

  private int computeDepthFromRoot(Comment parentComment) {
    int depth = 0;
    Set<String> visited = new HashSet<>();
    Comment current = parentComment;

    while (current.getParentId() != null) {
      if (!visited.add(current.getId())) {
        throw new IllegalArgumentException("COMMENT_004:허용 depth를 초과했습니다");
      }
      depth += 1;
      current = commentMapper.findByIdIncludingDeleted(current.getParentId())
          .orElseThrow(() -> new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다"));
    }
    return depth;
  }

  private String normalizeContent(String content) {
    return content
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase(Locale.ROOT);
  }

  private void guardAgainstDuplicateCommentWrite(String postId, String userId, String normalizedContent) {
    long now = System.currentTimeMillis();
    String key = postId + "|" + userId + "|" + normalizedContent;

    synchronized (duplicateCommentWriteCache) {
      Long previous = duplicateCommentWriteCache.get(key);
      if (previous != null && now - previous <= DUPLICATE_COMMENT_WINDOW_MILLIS) {
        throw new IllegalArgumentException("COMMENT_005:동일 내용 단기 중복 댓글은 잠시 후 다시 시도해주세요");
      }
      duplicateCommentWriteCache.put(key, now);
      cleanupDuplicateCommentWriteCache(now);
    }
  }

  private void cleanupDuplicateCommentWriteCache(long now) {
    if (duplicateCommentWriteCache.size() < 10_000) {
      return;
    }
    duplicateCommentWriteCache.entrySet().removeIf(
        entry -> now - entry.getValue() > DUPLICATE_COMMENT_WINDOW_MILLIS);
  }

  private String resolveActorName(String userId) {
    User actor = userMapper.findById(userId);
    if (actor == null || actor.getNickname() == null || actor.getNickname().isBlank()) {
      return "누군가";
    }
    return actor.getNickname();
  }

  private String buildFeedLink(String challengeId, String postId) {
    return "/challenges/" + challengeId + "/feed?postId=" + postId;
  }

  /**
   * 챌린지 멤버 검증(LEFT 제외).
   */
  private Map<String, Object> requireReadableMember(String challengeId, String userId) {
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null || "LEFT".equals(asString(memberInfo.get("STATUS")))) {
      throw new IllegalArgumentException("MEMBER_001: 챌린지 멤버가 아닙니다");
    }
    return memberInfo;
  }

  private Map<String, Object> requireActiveMember(String challengeId, String userId) {
    Map<String, Object> memberInfo = requireReadableMember(challengeId, userId);
    if (!"ACTIVE".equals(asString(memberInfo.get("STATUS")))) {
      throw new IllegalArgumentException("MEMBER_001: 챌린지 멤버 권한이 없습니다");
    }
    return memberInfo;
  }

  /**
   * 게시글 존재 여부 + 챌린지 경계 검증.
   */
  private Post requirePostInChallenge(String challengeId, String postId) {
    Post post = postMapper.findById(postId);
    if (post == null || !challengeId.equals(post.getChallengeId())) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }
    return post;
  }

  /**
   * 댓글 존재 여부 + 게시글 경계 검증.
   */
  private Comment requireCommentInPost(String commentId, String postId) {
    Optional<Comment> commentOpt = commentMapper.findById(commentId);
    if (commentOpt.isEmpty()) {
      throw new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다");
    }

    Comment comment = commentOpt.get();
    if (!postId.equals(comment.getPostId())) {
      throw new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다");
    }
    return comment;
  }

  /**
   * null-safe 문자열 변환 유틸.
   */
  private String asString(Object value) {
    return value == null ? null : value.toString();
  }
}
