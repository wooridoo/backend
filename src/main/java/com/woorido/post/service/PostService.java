package com.woorido.post.service;

import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.notification.domain.NotificationType;
import com.woorido.notification.service.NotificationService;
import com.woorido.post.domain.Post;
import com.woorido.post.domain.PostFactory;
import com.woorido.post.domain.PostUpdateVisitor;
import com.woorido.post.domain.PostDeleteVisitor;
import com.woorido.post.dto.request.CreatePostRequest;
import com.woorido.post.dto.request.UpdatePostRequest;
import com.woorido.post.dto.response.CreatePostResponse;
import com.woorido.common.dto.AuthorInfo;
import com.woorido.post.dto.response.PostDetailResponse;
import com.woorido.post.dto.response.PinPostResponse;
import com.woorido.post.repository.PostMapper;
import java.util.List;
import java.util.Map;
import com.woorido.post.dto.response.PostListResponse;
import com.woorido.post.dto.response.PostSummaryResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
  // 학습 포인트:
  // - 생성/수정은 Factory + Visitor 패턴으로 도메인 변경 책임을 분리한다.
  // - 삭제 권한은 Strategy로 분리해 서비스 로직을 단순화한다.

  private final PostMapper postMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final UserMapper userMapper;
  private final PostFactory postFactory;
  private final com.woorido.post.repository.PostImageMapper postImageMapper;
  private final com.woorido.post.repository.PostLikeMapper postLikeMapper;
  private final com.woorido.post.domain.PostLikeFactory postLikeFactory;
  private final com.woorido.post.domain.PostImageFactory postImageFactory;
  private final com.woorido.post.domain.PostDeleteStrategy postDeleteStrategy;
  private final SocialRateLimitService socialRateLimitService;
  private final NotificationService notificationService;
  private static final int MAX_POST_IMAGE_COUNT = 10;
  private static final Set<String> ALLOWED_POST_CATEGORIES = Set.of("GENERAL", "NOTICE", "QUESTION");

  /**
   * 게시글 생성.
   * 흐름: 멤버 검증 -> 공지 권한 검증 -> 게시글/이미지 저장 -> 응답 생성
   */
  public CreatePostResponse createPost(String challengeId, String userId, CreatePostRequest request) {
    // 챌린지 멤버 여부(LEFT 제외) 검증
    Map<String, Object> memberInfo = requireActiveMember(challengeId, userId);

    String role = (String) memberInfo.get("ROLE");
    String normalizedCategory = normalizeCategory(request.getCategory());

    // 공지 글은 리더만 작성 가능
    boolean isNotice = "NOTICE".equals(normalizedCategory);
    if (isNotice) {
      if (!"LEADER".equals(role)) {
        throw new IllegalArgumentException("POST_002:공지 게시글은 리더만 작성할 수 있습니다");
      }
    }

    String isNoticeFlag = isNotice ? "Y" : "N";
    String isPinnedFlag = isNotice ? "Y" : "N";

    // 도메인 팩토리로 게시글 엔티티 생성
    Post post = postFactory.create(challengeId, userId, request, normalizedCategory, isNoticeFlag, isPinnedFlag);

    validateImageUrls(request.getImageUrls());

    try {
      // 공지 작성 시 기존 공지 고정을 해제해 챌린지당 1개 고정 정책을 보장한다.
      if (isNotice) {
        postMapper.clearPinnedNotices(challengeId);
      }

      // 게시글 본문 저장
      postMapper.insert(post);

      // 첨부 이미지 메타데이터 저장
      saveImages(post.getId(), request.getImageUrls());
    } catch (DataAccessException e) {
      handlePostWriteFailure(challengeId, userId, role, normalizedCategory, e);
    }

    // 작성자 정보 포함 응답 구성
    User user = userMapper.findById(userId);

    return CreatePostResponse.builder()
        .postId(post.getId())
        .title(post.getTitle())
        .category(post.getCategory())
        .author(AuthorInfo.builder()
            .userId(userId)
            .nickname(user != null ? user.getNickname() : "Unknown")
            .profileImage(user != null ? user.getProfileImageUrl() : null)
            .build())
        .createdAt(post.getCreatedAt())
        .updatedAt(post.getCreatedAt())
        .content(post.getContent())
        .build();
  }

  /**
   * 게시글 수정.
   * 흐름: 멤버/대상 게시글 검증 -> 작성자 검증 -> 공지 권한 검증 -> 수정 반영
   */
  public CreatePostResponse updatePost(String challengeId, String postId, String userId, UpdatePostRequest request) {
    // 챌린지 멤버 여부(LEFT 제외) 검증
    Map<String, Object> memberInfo = requireActiveMember(challengeId, userId);
    String role = (String) memberInfo.get("ROLE");

    // 수정 대상 게시글 조회 및 경계(challengeId) 검증
    Post post = postMapper.findById(postId);
    if (post == null) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }
    if (!post.getChallengeId().equals(challengeId)) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }

    // 작성자 본인만 수정 가능
    if (!post.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("POST_004:게시글 수정 권한이 없습니다");
    }

    // category 미입력 시 기존 값을 유지한다.
    String nextCategory = request.getCategory() != null
        ? normalizeCategory(request.getCategory())
        : normalizeCategory(post.getCategory());
    boolean isNotice = "NOTICE".equals(nextCategory);
    if (isNotice) {
      if (!"LEADER".equals(role)) {
        throw new IllegalArgumentException("POST_002:공지 게시글은 리더만 작성할 수 있습니다");
      }
    }
    request.setCategory(nextCategory);

    // Visitor 패턴으로 변경값 반영
    PostUpdateVisitor visitor = new PostUpdateVisitor(request, isNotice ? "Y" : "N");
    post.accept(visitor);

    validateImageUrls(request.getImageUrls());

    try {
      // 게시글 본문 업데이트
      postMapper.update(post);

      if (isNotice) {
        // 공지 상태라면 항상 단일 고정 정책을 맞춘다.
        postMapper.clearPinnedNotices(challengeId);
        postMapper.updatePinned(postId, "Y");
      } else {
        // 일반 게시글은 고정 상태를 유지하지 않는다.
        postMapper.updatePinned(postId, "N");
      }

      // 이미지 목록은 전체 교체 방식으로 동기화
      postImageMapper.deleteAllByPostId(postId);
      saveImages(postId, request.getImageUrls());
    } catch (DataAccessException e) {
      handlePostWriteFailure(challengeId, userId, role, nextCategory, e);
    }

    User user = userMapper.findById(userId);
    return CreatePostResponse.builder()
        .postId(post.getId())
        .title(post.getTitle())
        .category(post.getCategory())
        .author(AuthorInfo.builder()
            .userId(userId)
            .nickname(user != null ? user.getNickname() : "Unknown")
            .profileImage(user != null ? user.getProfileImageUrl() : null)
            .build())
        // 수정 응답에서도 createdAt은 원본 생성 시각을 그대로 유지한다.
        .createdAt(post.getCreatedAt())
        .updatedAt(post.getUpdatedAt())
        .content(post.getContent())
        .build();
  }

  /**
   * 게시글 상세 조회.
   * 흐름: 멤버 검증 -> 조회수 증가 -> 상세 조회 -> 좋아요/이미지 포함 응답 구성
   */
  public PostDetailResponse getPostDetail(String challengeId, String postId, String userId) {
    // 조회 권한(멤버십) 검증
    requireReadableMember(challengeId, userId);

    // 조회수 증가
    postMapper.increaseViewCount(postId);

    // 작성자 조인 포함 상세 데이터 조회
    Map<String, Object> postMap = postMapper.findByIdWithAuthor(postId);
    if (postMap == null) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }

    // 경로 challengeId와 실제 게시글 소속 challengeId 일치 검증
    if (!challengeId.equals(postMap.get("CHALLENGE_ID"))) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }

    // null-safe 기본값 처리
    String title = (String) postMap.get("TITLE");
    String content = (String) postMap.get("CONTENT");
    String category = (String) postMap.get("CATEGORY");

    if (title == null)
      title = "";
    if (category == null)
      category = "GENERAL";

    // 로그인 사용자의 좋아요 여부/이미지 목록 조회
    boolean isLiked = postMapper.isLiked(postId, userId);
    List<com.woorido.post.domain.PostImage> images = postImageMapper.findAllByPostId(postId);

    // 응답 DTO 조립
    return PostDetailResponse.builder()
        .postId((String) postMap.get("ID"))
        .title(title)
        .content(content)
        .category(category)
        .author(AuthorInfo.builder()
            .userId((String) postMap.get("CREATED_BY"))
            .nickname((String) postMap.get("AUTHOR_NICKNAME"))
            .profileImage((String) postMap.get("AUTHOR_PROFILE_IMAGE"))
            .build())
        .images(images.stream()
            .map(img -> PostDetailResponse.ImageInfo.builder()
                .id(img.getId())
                .url(img.getImageUrl())
                .displayOrder(img.getDisplayOrder())
                .build())
            .toList())
        .likeCount(((Number) postMap.get("LIKE_COUNT")).longValue())
        .commentCount(((Number) postMap.get("COMMENT_COUNT")).longValue())
        .viewCount(((Number) postMap.get("VIEW_COUNT")).longValue())
        .isLiked(isLiked)
        .isPinned("Y".equals(postMap.get("IS_PINNED")))

        .createdAt(toLocalDateTime(postMap.get("CREATED_AT")))
        .updatedAt(toLocalDateTime(postMap.get("UPDATED_AT")))
        .build();
  }

  /**
   * 이미지 URL 목록을 게시글 이미지 엔티티로 저장한다.
   */
  private void saveImages(String postId, List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }
    for (int i = 0; i < imageUrls.size(); i++) {
      String url = imageUrls.get(i);
      com.woorido.post.domain.PostImage image = postImageFactory.create(postId, url, i);
      postImageMapper.save(image);
    }
  }

  /**
   * 게시글 목록 조회.
   * 흐름: 멤버 검증 -> 필터/정렬/페이징 파라미터 구성 -> 목록/카운트 조회 -> DTO 변환
   */
  public PostListResponse getPostList(String challengeId, String userId, int page, int size, String category,
      String sortBy, String order) {
    // 챌린지 멤버 여부(LEFT 제외) 검증
    requireReadableMember(challengeId, userId);

    // 동적 SQL 파라미터 구성
    Map<String, Object> params = new HashMap<>();
    params.put("challengeId", challengeId);
    params.put("userId", userId);

    // 카테고리 필터
    if ("NOTICE".equals(category)) {
      params.put("isNotice", "Y");
    } else if ("GENERAL".equals(category) || "QUESTION".equals(category)) {
      params.put("isNotice", "N");
    }
    // ALL/null인 경우 카테고리 필터 미적용

    // 정렬 컬럼/정렬 방향 결정
    String sortColumn = "created_at";
    if ("LIKES".equals(sortBy))
      sortColumn = "like_count";
    else if ("COMMENTS".equals(sortBy))
      sortColumn = "comment_count";

    String sortOrder = "DESC";
    if ("ASC".equalsIgnoreCase(order))
      sortOrder = "ASC";

    params.put("sortColumn", sortColumn);
    params.put("sortOrder", sortOrder);

    // 페이징 범위 계산(rownum/offset용)
    // page=0, size=20이면 0~19 구간을 의미한다.
    int startRow = page * size;
    int endRow = (page + 1) * size;
    params.put("startRow", startRow);
    params.put("endRow", endRow);

    // 데이터 조회
    int totalElements = postMapper.count(params);
    List<Map<String, Object>> posts = Collections.emptyList();

    if (totalElements > 0) {
      posts = postMapper.findAll(params);
    }

    // 조회 결과를 응답 DTO로 변환
    List<PostSummaryResponse> contentList = posts.stream().map(p -> {
      String title = (String) p.get("TITLE");
      String content = (String) p.get("CONTENT");
      String categoryVal = (String) p.get("CATEGORY");

      if (title == null)
        title = "";
      if (categoryVal == null)
        categoryVal = "GENERAL";

      // JDBC 드라이버별 숫자 타입 차이를 흡수하는 변환 로직
      boolean isLikedVal = false;
      Object isLikedObj = p.get("IS_LIKED");
      if (isLikedObj instanceof Number) {
        isLikedVal = ((Number) isLikedObj).intValue() > 0;
      }

      return PostSummaryResponse.builder()
          .postId((String) p.get("ID"))
          .title(title)
          .content(content)
          .category(categoryVal)
          .author(AuthorInfo.builder()
              .userId((String) p.get("CREATED_BY"))
              .nickname((String) p.get("AUTHOR_NICKNAME"))
              .profileImage((String) p.get("AUTHOR_PROFILE_IMAGE"))
              .build())
          .likeCount(((Number) p.get("LIKE_COUNT")).longValue())
          .commentCount(((Number) p.get("COMMENT_COUNT")).longValue())
          .viewCount(((Number) p.get("VIEW_COUNT")).longValue())
          .isPinned("Y".equals(p.get("IS_PINNED")))
          .isLiked(isLikedVal)
          .images(getImageUrlsForPost((String) p.get("ID")))
          .createdAt(toLocalDateTime(p.get("CREATED_AT")))
          .build();
    }).toList();

    return PostListResponse.builder()
        .content(contentList)
        .totalElements(totalElements)
        .totalPages((int) Math.ceil((double) totalElements / size))
        .number(page)
        .size(size)
        .build();
  }

  /**
   * 공지 게시글 상단 고정/해제.
   * 정책: NOTICE만 고정 가능, 챌린지당 고정은 1건만 허용, 리더만 변경 가능.
   */
  public PinPostResponse setPostPinned(String challengeId, String postId, String userId, boolean pinned) {
    Map<String, Object> memberInfo = requireActiveMember(challengeId, userId);
    String role = (String) memberInfo.get("ROLE");
    if (!"LEADER".equals(role)) {
      throw new IllegalArgumentException("POST_002:공지 게시글 고정 권한이 없습니다");
    }

    Post post = postMapper.findById(postId);
    if (post == null || !challengeId.equals(post.getChallengeId())) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }

    if (!"Y".equals(post.getIsNotice())) {
      throw new IllegalArgumentException("POST_005:공지 게시글만 고정할 수 있습니다");
    }

    if (pinned) {
      postMapper.clearPinnedNotices(challengeId);
      postMapper.updatePinned(postId, "Y");
    } else {
      postMapper.updatePinned(postId, "N");
    }

    return PinPostResponse.builder()
        .postId(postId)
        .isPinned(pinned)
        .pinnedAt(LocalDateTime.now())
        .build();
  }

  /**
   * DB 타임스탬프 값을 LocalDateTime으로 안전하게 변환한다.
   */
  private LocalDateTime toLocalDateTime(Object timestampObj) {
    if (timestampObj == null) {
      return null;
    }
    if (timestampObj instanceof java.sql.Timestamp) {
      return ((java.sql.Timestamp) timestampObj).toLocalDateTime();
    }
    if (timestampObj instanceof java.time.LocalDateTime) {
      return (java.time.LocalDateTime) timestampObj;
    }
    // Oracle 전용 타입을 리플렉션으로 처리(컴파일 의존성 분리)
    try {
      if (timestampObj.getClass().getName().equals("oracle.sql.TIMESTAMP")) {
        java.lang.reflect.Method method = timestampObj.getClass().getMethod("timestampValue");
        java.sql.Timestamp ts = (java.sql.Timestamp) method.invoke(timestampObj);
        return ts.toLocalDateTime();
      }
    } catch (Exception e) {
      log.error("Failed to convert Oracle TIMESTAMP via reflection", e);
    }
    return null;

  }

  /**
   * 게시글 좋아요 토글.
   * 이미 좋아요가 있으면 취소, 없으면 생성한다.
   */
  public com.woorido.post.dto.response.PostLikeResponse toggleLike(
      String challengeId,
      String postId,
      String userId,
      String clientIp) {
    socialRateLimitService.checkLikeWriteLimit(userId, clientIp);

    Post post = postMapper.findById(postId);
    if (post == null || !challengeId.equals(post.getChallengeId())) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }

    // 좋아요는 챌린지 멤버만 가능하도록 조회 API와 동일한 권한 경계를 맞춘다.
    requireActiveMember(challengeId, userId);

    boolean liked;
    if (postLikeMapper.exists(postId, userId)) {
      postLikeMapper.delete(postId, userId);
      postMapper.decreaseLikeCount(postId);
      liked = false;
    } else {
      com.woorido.post.domain.PostLike newLike = postLikeFactory.create(postId, userId);
      postLikeMapper.save(newLike);
      postMapper.increaseLikeCount(postId);
      liked = true;
    }

    post = postMapper.findById(postId);
    if (liked && post != null) {
      notificationService.publishSocialNotification(
          NotificationType.POST_LIKED,
          post.getCreatedBy(),
          userId,
          postId,
          "게시글 좋아요",
          resolveActorName(userId) + "님이 회원님의 게시글을 좋아합니다.",
          "회원님의 게시글에 좋아요가 도착했습니다.",
          buildFeedLink(challengeId, postId),
          "POST",
          postId);
    }

    return com.woorido.post.dto.response.PostLikeResponse.builder()
        .postId(postId)
        .liked(liked)
        .likeCount(post.getLikeCount())
        .build();
  }

  /**
   * 게시글 좋아요 취소.
   * 좋아요가 없으면 no-op으로 현재 상태를 반환한다.
   */
  public com.woorido.post.dto.response.PostLikeResponse unlikePost(
      String challengeId,
      String postId,
      String userId,
      String clientIp) {
    socialRateLimitService.checkLikeWriteLimit(userId, clientIp);

    Post post = postMapper.findById(postId);
    if (post == null || !challengeId.equals(post.getChallengeId())) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }

    requireActiveMember(challengeId, userId);

    if (postLikeMapper.exists(postId, userId)) {
      postLikeMapper.delete(postId, userId);
      postMapper.decreaseLikeCount(postId);
    }

    post = postMapper.findById(postId);
    return com.woorido.post.dto.response.PostLikeResponse.builder()
        .postId(postId)
        .liked(false)
        .likeCount(post != null ? post.getLikeCount() : 0)
        .build();
  }

  /**
   * 게시글 삭제(소프트 삭제).
   * 삭제 가능 여부는 전략 객체에서 검증한다.
   */
  public com.woorido.post.dto.response.DeletePostResponse deletePost(String challengeId, String postId, String userId) {
    Post post = postMapper.findById(postId);
    if (post == null) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }
    if (!post.getChallengeId().equals(challengeId)) {
      throw new IllegalArgumentException("POST_001:게시글을 찾을 수 없습니다");
    }

    // 요청자 역할 조회(삭제 권한 판단에 사용)
    Map<String, Object> memberInfo = requireActiveMember(challengeId, userId);
    String role = (String) memberInfo.get("ROLE");

    // 작성자/리더 조건 등 삭제 정책 검증
    postDeleteStrategy.validate(post, userId, role);

    // 도메인 상태값(삭제 시각 등) 갱신
    PostDeleteVisitor visitor = new PostDeleteVisitor();
    post.accept(visitor);

    // DB 반영(소프트 삭제)
    postMapper.delete(postId);

    return com.woorido.post.dto.response.DeletePostResponse.builder()
        .postId(postId)
        .deletedAt(post.getDeletedAt())
        .build();
  }

  /**
   * 챌린지 멤버(LEFT 제외) 검증 공통 메서드.
   */
  private Map<String, Object> requireReadableMember(String challengeId, String userId) {
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    // 탈퇴(LEFT) 상태는 조회/작성 모두 차단한다.
    if (memberInfo == null || "LEFT".equals(memberInfo.get("STATUS"))) {
      throw new IllegalArgumentException("MEMBER_001:챌린지 멤버가 아닙니다");
    }
    return memberInfo;
  }

  private Map<String, Object> requireActiveMember(String challengeId, String userId) {
    Map<String, Object> memberInfo = requireReadableMember(challengeId, userId);
    if (!"ACTIVE".equals(memberInfo.get("STATUS"))) {
      throw new IllegalArgumentException("MEMBER_001:챌린지 멤버 권한이 없습니다");
    }
    return memberInfo;
  }

  private void validateImageUrls(List<String> imageUrls) {
    if (imageUrls == null) {
      return;
    }

    if (imageUrls.size() > MAX_POST_IMAGE_COUNT) {
      throw new IllegalArgumentException("IMAGE_004:게시글 이미지는 최대 10장까지 업로드할 수 있습니다");
    }
  }

  private String normalizeCategory(String rawCategory) {
    String normalized = rawCategory == null ? "GENERAL" : rawCategory.trim().toUpperCase(Locale.ROOT);
    if (!ALLOWED_POST_CATEGORIES.contains(normalized)) {
      throw new IllegalArgumentException("VALIDATION_001:지원하지 않는 게시글 유형입니다");
    }
    return normalized;
  }

  private void handlePostWriteFailure(
      String challengeId,
      String userId,
      String userRole,
      String category,
      DataAccessException exception) {
    String dbErrorCode = resolveDbErrorCode(exception);
    log.error(
        "Post write failed. challengeId={}, userId={}, userRole={}, category={}, dbErrorCode={}",
        challengeId,
        userId,
        userRole,
        category,
        dbErrorCode,
        exception);
    throw new RuntimeException("POST_006:게시글 저장 중 오류가 발생했습니다", exception);
  }

  private String resolveDbErrorCode(Throwable throwable) {
    Throwable cursor = throwable;
    while (cursor != null) {
      if (cursor instanceof java.sql.SQLException sqlException) {
        String state = sqlException.getSQLState();
        if (state != null && !state.isBlank()) {
          return state;
        }
        return "SQL-" + sqlException.getErrorCode();
      }
      cursor = cursor.getCause();
    }
    return "DB_UNKNOWN";
  }

  private List<String> getImageUrlsForPost(String postId) {
    List<Map<String, Object>> attachments = postMapper.findAttachments(postId);
    if (attachments == null || attachments.isEmpty()) {
      return Collections.emptyList();
    }

    return attachments.stream()
        .map(row -> row.get("FILE_URL"))
        .filter(value -> value != null)
        .map(Object::toString)
        .toList();
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
}


