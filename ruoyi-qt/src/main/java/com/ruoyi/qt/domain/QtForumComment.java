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
    /** 当前社区昵称，查询时关联档案，不落库 */
    private String authorCommunityNickname;
    /** 评论人当前头像，查询时关联 sys_user，不落库 */
    private String authorAvatar;
    /** 被回复用户。父评论删除后仍用来读取当前昵称 */
    private Long replyToUserId;
    /** 被回复人姓名快照：对方账号不存在时的回退 */
    private String replyToUserName;
    /** 被回复人当前社区昵称，查询时关联档案，不落库 */
    private String replyToCommunityNickname;
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

    public String getAuthorCommunityNickname()
    {
        return authorCommunityNickname;
    }

    public void setAuthorCommunityNickname(String authorCommunityNickname)
    {
        this.authorCommunityNickname = authorCommunityNickname;
    }

    public String getAuthorAvatar()
    {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar)
    {
        this.authorAvatar = authorAvatar;
    }

    public Long getReplyToUserId()
    {
        return replyToUserId;
    }

    public void setReplyToUserId(Long replyToUserId)
    {
        this.replyToUserId = replyToUserId;
    }

    public String getReplyToUserName()
    {
        return replyToUserName;
    }

    public void setReplyToUserName(String replyToUserName)
    {
        this.replyToUserName = replyToUserName;
    }

    public String getReplyToCommunityNickname()
    {
        return replyToCommunityNickname;
    }

    public void setReplyToCommunityNickname(String replyToCommunityNickname)
    {
        this.replyToCommunityNickname = replyToCommunityNickname;
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
