package com.woorido.post.service;

import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.post.domain.Post;
import com.woorido.post.dto.request.CreatePostRequest;
import com.woorido.post.dto.response.CreatePostResponse;
import com.woorido.post.dto.response.PostDetailResponse;
import com.woorido.post.repository.PostMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.woorido.post.dto.response.PostListResponse;
import com.woorido.post.dto.response.PostSummaryResponse;
import java.util.Collections;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.woorido.post.repository.PostLikeMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

  private final PostMapper postMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final UserMapper userMapper;
  private final PostLikeMapper postLikeMapper;
  // private final PostImageMapper postImageMapper; // Uncomment if handling
  // images

  public void updatePost(String challengeId, String userId, String postId,
      com.woorido.post.dto.request.UpdatePostRequest request) {
    Post post = postMapper.findById(postId);
    if (post == null || !post.getChallengeId().equals(challengeId)) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }
    if (!post.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("POST_004: 수정 권한이 없습니다");
    }

    boolean isNotice = "NOTICE".equals(request.getCategory());
    if (isNotice) {
      Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
      String role = memberInfo != null ? (String) memberInfo.get("ROLE") : null;
      if (!"LEADER".equals(role)) {
        throw new IllegalArgumentException("POST_002: 공지사항은 모임장만 작성할 수 있습니다");
      }
    }

    String finalContent = request.getTitle() + "\n" + request.getContent();

    Post updatedPost = Post.builder()
        .id(postId)
        .content(finalContent)
        .isNotice(isNotice ? "Y" : "N")
        .isPinned(post.getIsPinned())
        .updatedAt(LocalDateTime.now())
        .build();

    postMapper.update(updatedPost);
  }

  public void deletePost(String challengeId, String postId, String userId) {
    Post post = postMapper.findById(postId);
    if (post == null || !post.getChallengeId().equals(challengeId)) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }
    // 작성자 또는 LEADER 삭제 허용 (PM-004, spec 061)
    if (!post.getCreatedBy().equals(userId)) {
      Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
      String role = memberInfo != null ? (String) memberInfo.get("ROLE") : null;
      if (!"LEADER".equals(role)) {
        throw new IllegalArgumentException("POST_004: 삭제 권한이 없습니다");
      }
    }
    postMapper.delete(postId);
  }

  @Transactional
  public Map<String, Object> toggleLike(String postId, String userId) {
    // 비관적 락으로 동시성 제어
    Post post = postMapper.findByIdForUpdate(postId);
    if (post == null) {
      throw new IllegalArgumentException("POST_001: 게시글을 찾을 수 없습니다");
    }

    boolean liked;
    if (postLikeMapper.exists(postId, userId)) {
      postLikeMapper.delete(postId, userId);
      postMapper.decreaseLikeCount(postId);
      liked = false;
    } else {
      postLikeMapper.save(com.woorido.post.domain.PostLike.builder()
          .id(UUID.randomUUID().toString())
          .postId(postId)
          .userId(userId)
          .createdAt(LocalDateTime.now())
          .build());
      postMapper.increaseLikeCount(postId);
      liked = true;
    }

    // spec 062: {postId, liked, likeCount}
    long newLikeCount = liked ? post.getLikeCount() + 1 : Math.max(post.getLikeCount() - 1, 0);
    Map<String, Object> result = new HashMap<>();
    result.put("postId", postId);
    result.put("liked", liked);
    result.put("likeCount", newLikeCount);
    return result;
  }

  public CreatePostResponse createPost(String challengeId, String userId, CreatePostRequest request) {
    // 1. Check Membership
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      // Using generic exceptions for now, or maybe
      // IllegalArgumentException/SecurityException
      // Ideally should use custom exception MEMBER_001
      throw new IllegalArgumentException("MEMBER_001: 챌린지에 참여하지 않았습니다");
    }

    String role = (String) memberInfo.get("ROLE"); // Assuming 'ROLE' key exists and is String.
                                                   // DB convention is usually snake_case, but mapper result maps are
                                                   // often camelCase if configured, or UPPERCASE keys.
                                                   // Safer to check for variations or use a helper if unsure.
                                                   // Assuming UPPERCASE "ROLE" matching column name.

    // 2. Validate Category & Role
    boolean isNotice = "NOTICE".equals(request.getCategory());
    if (isNotice) {
      if (!"LEADER".equals(role)) {
        throw new IllegalArgumentException("POST_002: 공지사항은 모임장만 작성할 수 있습니다");
      }
    }

    // 3. Merge Title + Content
    String finalContent = request.getTitle() + "\n" + request.getContent();

    // 4. Create Post Entity
    String postId = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();
    Post post = Post.builder()
        .id(postId)
        .challengeId(challengeId)
        .createdBy(userId)
        .content(finalContent)
        .isNotice(isNotice ? "Y" : "N")
        .isPinned("N")
        .createdAt(now)
        .updatedAt(now)
        .build();

    postMapper.insert(post);

    // 5. Build Response
    User user = userMapper.findById(userId);

    return CreatePostResponse.builder()
        .postId(postId)
        .title(request.getTitle())
        .category(isNotice ? "NOTICE" : "GENERAL") // Mapped back
        .author(CreatePostResponse.AuthorInfo.builder()
            .userId(userId)
            .nickname(user != null ? user.getNickname() : "Unknown")
            .build())
        .createdAt(post.getCreatedAt())
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

    // 4. Split Title & Content
    String fullContent = (String) postMap.get("CONTENT");
    String title = "";
    String content = fullContent;
    if (fullContent != null) {
      int firstNewLine = fullContent.indexOf("\n");
      if (firstNewLine > -1) {
        title = fullContent.substring(0, firstNewLine);
        content = fullContent.substring(firstNewLine + 1);
      } else {
        title = fullContent;
        content = "";
      }
    }

    // 5. Map Category
    String isNotice = (String) postMap.get("IS_NOTICE");
    String category = "Y".equals(isNotice) ? "NOTICE" : "GENERAL";

    // 6. Check Liked & Attachments
    boolean isLiked = postMapper.isLiked(postId, userId);
    List<Map<String, Object>> attachments = postMapper.findAttachments(postId);

    // 7. Build Response
    return PostDetailResponse.builder()
        .postId((String) postMap.get("ID"))
        .title(title)
        .content(content)
        .category(category)
        .author(CreatePostResponse.AuthorInfo.builder()
            .userId((String) postMap.get("CREATED_BY"))
            .nickname((String) postMap.get("AUTHOR_NICKNAME"))
            .build())
        .attachments(attachments.stream()
            .map(att -> PostDetailResponse.AttachmentInfo.builder()
                .fileId((String) att.get("FILE_ID"))
                .fileName((String) att.get("FILE_URL")) // Assuming URL contains name or just mapping URL for now.
                                                        // Wait, findAttachments selects image_url as FILE_URL. Where is
                                                        // name?
                                                        // Schema has post_images(id, post_id, image_url,
                                                        // display_order). No file name.
                                                        // So just use URL for both or leave name empty?
                                                        // Let's use URL as name for now or substring.
                .fileUrl((String) att.get("FILE_URL"))
                .fileSize(0L) // Schema doesn't have size
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
      String fullContent = (String) p.get("CONTENT");
      String title = "";
      String content = fullContent;
      if (fullContent != null) {
        int firstNewLine = fullContent.indexOf("\n");
        if (firstNewLine > -1) {
          title = fullContent.substring(0, firstNewLine);
          content = fullContent.substring(firstNewLine + 1);
        } else {
          title = fullContent;
          content = "";
        }
      }
      // Preview content (e.g. first 100 chars)? Spec says "Content" but usually list
      // has summary.
      // Spec Return table says "Content". Let's give full or split content.
      // If preview needed: if (content.length() > 100) content = content.substring(0,
      // 100) + "...";

      String isNotice = (String) p.get("IS_NOTICE");
      String cat = "Y".equals(isNotice) ? "NOTICE" : "GENERAL";

      return PostSummaryResponse.builder()
          .postId((String) p.get("ID"))
          .title(title)
          .content(content) // Sending rest of content as per split logic
          .category(cat)
          .author(CreatePostResponse.AuthorInfo.builder()
              .userId((String) p.get("CREATED_BY"))
              .nickname((String) p.get("AUTHOR_NICKNAME"))
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
}
