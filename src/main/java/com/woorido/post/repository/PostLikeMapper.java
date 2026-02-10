package com.woorido.post.repository;

import com.woorido.post.domain.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper {
    void save(PostLike postLike);

    void delete(@Param("postId") String postId, @Param("userId") String userId);

    boolean exists(@Param("postId") String postId, @Param("userId") String userId);

    long countByPostId(@Param("postId") String postId);
}
