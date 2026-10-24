package com.ruoyi.qt.domain;

/** 当前登录用户的帖子互动快照；不暴露收藏人数或其他用户身份。 */
public class QtForumReaction
{
    private Long postId;
    private long likeCount;
    private boolean liked;
    private boolean favorited;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }
}
