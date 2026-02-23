package com.woorido.post.repository;

import com.woorido.post.domain.Post;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper {
  void insert(Post post);

  Post findById(String id);

  // 상세 조회용 (작성자 정보 포함)
  Map<String, Object> findByIdWithAuthor(@Param("id") String id);

  void increaseViewCount(@Param("id") String id);

  void increaseLikeCount(@Param("postId") String postId);

  void decreaseLikeCount(@Param("postId") String postId);

  void increaseCommentCount(@Param("postId") String postId);

  void decreaseCommentCount(@Param("postId") String postId);

  boolean isLiked(@Param("postId") String postId, @Param("userId") String userId);

  List<Map<String, Object>> findAttachments(@Param("postId") String postId);

  // 목록 조회용
  List<Map<String, Object>> findAll(Map<String, Object> params);

  // 비관적 락 조회 (좋아요 토글 동시성 제어)
  Post findByIdForUpdate(@Param("id") String id);

  void update(Post post);

  void updatePinned(@Param("postId") String postId, @Param("isPinned") String isPinned);

  void clearPinnedNotices(@Param("challengeId") String challengeId);

  void delete(@Param("postId") String postId);

  int count(Map<String, Object> params);
}
