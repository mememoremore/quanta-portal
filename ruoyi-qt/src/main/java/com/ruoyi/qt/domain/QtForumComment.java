package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 社区论坛评论 qt_forum_comment
 */
public class QtForumComment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long commentId;
    private Long postId;
    /** 顶层评论为 null */
    private Long parentId;
    private String content;
    private Long authorId;
    private String authorName;
    /** 被回复人姓名快照：父评论删除后回复仍能显示 */
    private String replyToUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public Long getCommentId()
    {
        return commentId;
    }

    public void setCommentId(Long commentId)
    {
        this.commentId = commentId;
    }

    public Long getPostId()
    {
        return postId;
    }

    public void setPostId(Long postId)
    {
        this.postId = postId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getAuthorId()
    {
        return authorId;
    }

    public void setAuthorId(Long authorId)
    {
        this.authorId = authorId;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getReplyToUserName()
    {
        return replyToUserName;
    }

    public void setReplyToUserName(String replyToUserName)
    {
        this.replyToUserName = replyToUserName;
    }

    public Date getCreatedAt()
    {
        return createdAt != null ? createdAt : getCreateTime();
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
        setCreateTime(createdAt);
    }
}
