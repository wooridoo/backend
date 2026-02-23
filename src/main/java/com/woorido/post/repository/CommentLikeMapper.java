package com.woorido.post.repository;

import com.woorido.post.domain.CommentLike;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentLikeMapper {
    void save(CommentLike commentLike);

    void delete(@Param("commentId") String commentId, @Param("userId") String userId);

    boolean exists(@Param("commentId") String commentId, @Param("userId") String userId);

    void deleteByCommentId(@Param("commentId") String commentId);

    long countByCommentId(@Param("commentId") String commentId);

    List<String> findLikedCommentIds(@Param("commentIds") List<String> commentIds, @Param("userId") String userId);
}
