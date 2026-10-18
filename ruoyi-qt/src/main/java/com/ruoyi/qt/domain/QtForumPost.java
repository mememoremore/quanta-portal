package com.ruoyi.qt.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 社区论坛帖子 qt_forum_post
 */
public class QtForumPost extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 帖子分类固定三档 */
    public static final Set<String> CATEGORIES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("PROJECT", "TECH", "LIFE")));

    private Long postId;
    private String title;
    private String category;
    /** Markdown 原文 */
    private String content;
    private Long authorId;
    private String authorName;
    /** 当前社区昵称，查询时关联档案，不落库 */
    private String authorCommunityNickname;
    private String authorDepartment;
    /** 作者当前头像，查询时关联 sys_user，不落库 */
    private String authorAvatar;
    private Long commentCount;
    @JsonProperty(access = Access.READ_ONLY)
    private Long viewCount;

    public Long getPostId()
    {
        return postId;
    }

    public void setPostId(Long postId)
    {
        this.postId = postId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
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

    public String getAuthorDepartment()
    {
        return authorDepartment;
    }

    public void setAuthorDepartment(String authorDepartment)
    {
        this.authorDepartment = authorDepartment;
    }

    public String getAuthorAvatar()
    {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar)
    {
        this.authorAvatar = authorAvatar;
    }

    public Long getCommentCount()
    {
        return commentCount;
    }

    public void setCommentCount(Long commentCount)
    {
        this.commentCount = commentCount;
    }

    public Long getViewCount()
    {
        return viewCount;
    }

    public void setViewCount(Long viewCount)
    {
        this.viewCount = viewCount;
    }
}
