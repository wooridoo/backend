package com.woorido.post.repository;

import com.woorido.post.domain.PostImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostImageMapper {
    void save(PostImage postImage);

    void deleteByPostId(String postId);

    List<PostImage> findAllByPostId(String postId);
}
