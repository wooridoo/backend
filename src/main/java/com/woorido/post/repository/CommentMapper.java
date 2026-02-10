package com.woorido.post.repository;

import com.woorido.post.domain.Comment;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {
    void save(Comment comment);

    List<Comment> findAllByPostId(String postId);

    Optional<Comment> findById(String id);

    // 비관적 락 조회 (좋아요 토글 동시성 제어)
    Comment findByIdForUpdate(@Param("id") String id);

    void deleteById(String id);

    void increaseLikeCount(String id);

    void decreaseLikeCount(String id);
}
