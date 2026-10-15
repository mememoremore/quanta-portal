package com.ruoyi.qt.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
    private String authorDepartment;
    private Long commentCount;

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

    public String getAuthorDepartment()
    {
        return authorDepartment;
    }

    public void setAuthorDepartment(String authorDepartment)
    {
        this.authorDepartment = authorDepartment;
    }

    public Long getCommentCount()
    {
        return commentCount;
    }

    public void setCommentCount(Long commentCount)
    {
        this.commentCount = commentCount;
    }
}
