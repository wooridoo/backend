package com.woorido.post.service;

import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.post.domain.Post;
import com.woorido.post.domain.PostFactory;
import com.woorido.post.domain.PostUpdateVisitor;
import com.woorido.post.domain.PostDeleteVisitor;
import com.woorido.post.dto.request.CreatePostRequest;
import com.woorido.post.dto.request.UpdatePostRequest;
import com.woorido.post.dto.response.CreatePostResponse;
import com.woorido.common.dto.AuthorInfo;
import com.woorido.post.dto.response.PostDetailResponse;
import com.woorido.post.repository.PostMapper;
import java.util.List;
import java.util.Map;
import com.woorido.post.dto.response.PostListResponse;
import com.woorido.post.dto.response.PostSummaryResponse;
import java.util.Collections;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

  private final PostMapper postMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final UserMapper userMapper;
  private final PostFactory postFactory;
  private final com.woorido.post.repository.PostImageMapper postImageMapper;
  private final com.woorido.post.repository.PostLikeMapper postLikeMapper;
  private final com.woorido.post.domain.PostLikeFactory postLikeFactory;
  private final com.woorido.post.domain.PostImageFactory postImageFactory;
  private final com.woorido.post.domain.PostDeleteStrategy postDeleteStrategy;

  public CreatePostResponse createPost(String challengeId, String userId, CreatePostRequest request) {
    // 1. Check Membership
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      throw new IllegalArgumentException("MEMBER_001: 챌린지에 참여하지 않았습니다");
    }

    String role = (String) memberInfo.get("ROLE");

    // 2. Validate Category & Role
    boolean isNotice = "NOTICE".equals(request.getCategory());
    if (isNotice) {
      if (!"LEADER".equals(role)) {
        throw new IllegalArgumentException("POST_002: 공지사항은 모임장만 작성할 수 있습니다");
      }
    }

    // 3. Create Post Entity using Factory
    Post post = postFactory.create(challengeId, userId, request, isNotice ? "Y" : "N");

    // 4. Insert
    postMapper.insert(post);

    // 4.1 Save Images
    saveImages(post.getId(), request.getImageUrls());

    // 5. Build Response
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

  public CreatePostResponse updatePost(String challengeId, String postId, String userId, UpdatePostRequest request) {
    // 1. Check Membership
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      throw new IllegalArgumentException("MEMBER_001: 챌린지에 참여하지 않았습니다");
    }
    String role = (String) memberInfo.get("ROLE");

    // 2. Find Post
    Post post = postMapper.findById(postId);
    if (post == null) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }
    if (!post.getChallengeId().equals(challengeId)) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }

    // 3. Check Author
    if (!post.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("POST_004: 수정 권한이 없습니다");
    }

    // 4. Validate Category & Permission
    String category = request.getCategory();
    boolean isNotice = "NOTICE".equals(category);
    if (isNotice) {
      if (!"LEADER".equals(role)) {
        throw new IllegalArgumentException("POST_002: 공지사항은 모임장만 작성할 수 있습니다");
      }
    }

    // 5. Update using Visitor
    PostUpdateVisitor visitor = new PostUpdateVisitor(request, isNotice ? "Y" : "N");
    post.accept(visitor);

    // 6. Update DB
    postMapper.update(post);

    // 6.1 Update Images
    postImageMapper.deleteAllByPostId(postId);
    saveImages(postId, request.getAttachmentIds());

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
        .createdAt(post.getCreatedAt()) // Return created_at for consistency or null? Spec says updatedAt for Update.
        .updatedAt(post.getUpdatedAt())
        .content(post.getContent())
        .build();
  }

  public PostDetailResponse getPostDetail(String challengeId, String postId, String userId) {
    // 1. Check Membership
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      throw new IllegalArgumentException("MEMBER_001: 챌린지에 참여하지 않았습니다");
    }

    // 2. Increase View Count
    postMapper.increaseViewCount(postId);

    // 3. Get Post Data
    Map<String, Object> postMap = postMapper.findByIdWithAuthor(postId);
    if (postMap == null) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }

    // Validate Challenge ID
    if (!challengeId.equals(postMap.get("CHALLENGE_ID"))) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }

    // 4. Use Title & Content directly
    String title = (String) postMap.get("TITLE");
    String content = (String) postMap.get("CONTENT");
    String category = (String) postMap.get("CATEGORY");

    if (title == null)
      title = "";
    if (category == null)
      category = "GENERAL";

    // 6. Check Liked & Images
    boolean isLiked = postMapper.isLiked(postId, userId);
    List<com.woorido.post.domain.PostImage> images = postImageMapper.findAllByPostId(postId);

    // 7. Build Response
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

  public PostListResponse getPostList(String challengeId, String userId, int page, int size, String category,
      String sortBy, String order) {
    // 1. Check Membership
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      throw new IllegalArgumentException("MEMBER_001: 챌린지에 참여하지 않았습니다");
    }

    // 2. Prepare Params
    Map<String, Object> params = new HashMap<>();
    params.put("challengeId", challengeId);

    // Category Filter
    if ("NOTICE".equals(category)) {
      params.put("isNotice", "Y");
    } else if ("GENERAL".equals(category) || "QUESTION".equals(category)) {
      params.put("isNotice", "N");
    }
    // if ALL or null, no isNotice filter

    // Sort
    String sortColumn = "created_at"; // default
    if ("LIKES".equals(sortBy))
      sortColumn = "like_count";
    else if ("COMMENTS".equals(sortBy))
      sortColumn = "comment_count";

    String sortOrder = "DESC"; // default
    if ("ASC".equalsIgnoreCase(order))
      sortOrder = "ASC";

    params.put("sortColumn", sortColumn);
    params.put("sortOrder", sortOrder);

    // Pagination (1-based rownum)
    int startRow = page * size;
    int endRow = (page + 1) * size;
    params.put("startRow", startRow);
    params.put("endRow", endRow);

    // 3. Get Data
    int totalElements = postMapper.count(params);
    List<Map<String, Object>> posts = Collections.emptyList();

    if (totalElements > 0) {
      posts = postMapper.findAll(params);
    }

    // 4. Map to DTO
    List<PostSummaryResponse> contentList = posts.stream().map(p -> {
      String title = (String) p.get("TITLE");
      String content = (String) p.get("CONTENT");
      String categoryVal = (String) p.get("CATEGORY");

      if (title == null)
        title = "";
      if (categoryVal == null)
        categoryVal = "GENERAL";

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
    // Handle Oracle specific type by name to avoid direct dependency if possible
    try {
      if (timestampObj.getClass().getName().equals("oracle.sql.TIMESTAMP")) {
        // oracle.sql.TIMESTAMP has toLocalDateTime() or timestampValue()
        // Reflection to avoid compile error if dependency missing
        java.lang.reflect.Method method = timestampObj.getClass().getMethod("timestampValue");
        java.sql.Timestamp ts = (java.sql.Timestamp) method.invoke(timestampObj);
        return ts.toLocalDateTime();
      }
    } catch (Exception e) {
      log.error("Failed to convert Oracle TIMESTAMP via reflection", e);
    }
    return null;

  }

  public com.woorido.post.dto.response.PostLikeResponse toggleLike(String postId, String userId) {
    Post post = postMapper.findById(postId);
    if (post == null) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }

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

    // Refresh post to get updated count
    post = postMapper.findById(postId);

    return com.woorido.post.dto.response.PostLikeResponse.builder()
        .postId(postId)
        .liked(liked)
        .likeCount(post.getLikeCount())
        .build();
  }

  public com.woorido.post.dto.response.DeletePostResponse deletePost(String challengeId, String postId, String userId) {
    Post post = postMapper.findById(postId);
    if (post == null) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }
    if (!post.getChallengeId().equals(challengeId)) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }

    // 0. Fetch Member Role
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      throw new IllegalArgumentException("MEMBER_001: 챌린지에 참여하지 않았습니다");
    }
    String role = (String) memberInfo.get("ROLE");

    // 1. Validate using Strategy
    postDeleteStrategy.validate(post, userId, role);

    // 2. Update State using Visitor
    PostDeleteVisitor visitor = new PostDeleteVisitor();
    post.accept(visitor);

    // 3. Persist (Soft Delete)
    postMapper.delete(postId);

    return com.woorido.post.dto.response.DeletePostResponse.builder()
        .postId(postId)
        .deletedAt(post.getDeletedAt())
        .build();
  }
}
