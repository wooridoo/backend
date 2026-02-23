package com.woorido.post.repository;

import com.woorido.post.domain.Comment;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {
    void save(Comment comment);

    List<Comment> findAllByPostId(@Param("postId") String postId);

    void increaseLikeCount(@Param("commentId") String commentId);

    void decreaseLikeCount(@Param("commentId") String commentId);

    Optional<Comment> findById(@Param("id") String id);

    Optional<Comment> findByIdIncludingDeleted(@Param("id") String id);

    void deleteById(@Param("id") String id); // Soft delete handled in SQL

    void update(com.woorido.post.domain.Comment comment);

    int countByParentId(@Param("parentId") String parentId);

    void deletePhysical(@Param("id") String id);
}
