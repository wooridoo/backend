package com.woorido.post.repository;

import com.woorido.post.domain.PostImage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostImageMapper {
    void save(PostImage postImage);

    List<PostImage> findAllByPostId(String postId);

    void deleteAllByPostId(String postId);
}
