package com.ruoyi.qt.mapper;

import java.util.List;
import com.ruoyi.qt.domain.QtForumComment;

public interface QtForumCommentMapper
{
    List<QtForumComment> selectCommentList(QtForumComment query);

    QtForumComment selectCommentById(Long commentId);

    int insertComment(QtForumComment comment);

    int updateCommentContent(QtForumComment comment);

    int deleteCommentById(Long commentId);

    int promoteReplies(QtForumComment comment);

    int deleteByPostId(Long postId);
}
