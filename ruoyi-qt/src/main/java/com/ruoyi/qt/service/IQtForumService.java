package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtForumComment;
import com.ruoyi.qt.domain.QtForumPost;

public interface IQtForumService
{
    List<QtForumPost> selectPostList(QtForumPost query, boolean mine);

    List<QtForumPost> selectPostListByAuthor(Long authorId);

    QtForumPost selectPostById(Long postId);

    /** 详情访问：校验存在后原子递增浏览量并返回最新帖子 */
    QtForumPost viewPost(Long postId);

    QtForumPost insertPost(QtForumPost post);

    QtForumPost updatePost(Long postId, QtForumPost body);

    void deletePostById(Long postId);

    List<QtForumComment> selectCommentList(Long postId);

    QtForumComment insertComment(Long postId, QtForumComment body);

    QtForumComment updateComment(Long commentId, QtForumComment body);

    void deleteCommentById(Long commentId);

    boolean canModerate(Long authorId);
}
