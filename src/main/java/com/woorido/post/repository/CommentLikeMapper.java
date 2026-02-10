package com.woorido.post.repository;

import com.woorido.post.domain.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentLikeMapper {
    void save(CommentLike commentLike);
    boolean exists(@Param("commentId") String commentId, @Param("userId") String userId);
    void delete(@Param("commentId") String commentId, @Param("userId") String userId);
}
