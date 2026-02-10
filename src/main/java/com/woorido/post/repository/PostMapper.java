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

  boolean isLiked(@Param("postId") String postId, @Param("userId") String userId);

  List<Map<String, Object>> findAttachments(@Param("postId") String postId);

  // 목록 조회용
  List<Map<String, Object>> findAll(Map<String, Object> params);

  // 단건 조회 (Entity 반환 - 수정/삭제 등의 로직용)
  Post findById(@Param("id") String id);

  // 비관적 락 조회 (좋아요 토글 동시성 제어)
  Post findByIdForUpdate(@Param("id") String id);

  void update(Post post);

  void delete(@Param("id") String id);

  void increaseLikeCount(@Param("id") String id);

  void decreaseLikeCount(@Param("id") String id);

  int count(Map<String, Object> params);

  void update(Post post);

  void delete(@Param("postId") String postId);
}
