package com.ruoyi.qt.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /** MEMBERS: 全部塔员；MANAGERS: 经理及以上。空值只用于兼容旧编辑请求。 */
    private String visibility;
    @JsonIgnore
    private Long viewerId;

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Long getViewerId() { return viewerId; }
    public void setViewerId(Long viewerId) { this.viewerId = viewerId; }
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
    @JsonProperty(access = Access.READ_ONLY)
    private long likeCount;
    @JsonProperty(access = Access.READ_ONLY)
    private boolean liked;
    @JsonProperty(access = Access.READ_ONLY)
    private boolean favorited;
    @JsonProperty(access = Access.READ_ONLY)
    private boolean reactionsReady;

    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }
    public boolean isReactionsReady() { return reactionsReady; }
    public void setReactionsReady(boolean reactionsReady) { this.reactionsReady = reactionsReady; }

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
