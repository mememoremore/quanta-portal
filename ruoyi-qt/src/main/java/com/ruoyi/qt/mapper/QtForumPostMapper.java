package com.ruoyi.qt.mapper;

import java.util.List;
import com.ruoyi.qt.domain.QtForumPost;

public interface QtForumPostMapper
{
    List<QtForumPost> selectPostList(QtForumPost query);

    QtForumPost selectPostById(Long postId);

    int insertPost(QtForumPost post);

    int updatePost(QtForumPost post);

    int deletePostById(Long postId);

    int refreshCommentCount(Long postId);

    int incrementViewCount(Long postId);
}
