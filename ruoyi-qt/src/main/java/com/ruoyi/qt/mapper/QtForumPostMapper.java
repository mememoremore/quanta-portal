package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtForumPost;

public interface QtForumPostMapper
{
    List<QtForumPost> selectPostList(QtForumPost query);

    QtForumPost selectPostById(Long postId);

    QtForumPost lockPostById(Long postId);

    List<QtForumPost> selectPersonalPostList(@Param("category") String category,
            @Param("userId") Long userId, @Param("kind") String kind);

    int insertPost(QtForumPost post);

    int updatePost(QtForumPost post);

    int deletePostById(Long postId);

    int refreshCommentCount(Long postId);

    int incrementViewCount(Long postId);
}
