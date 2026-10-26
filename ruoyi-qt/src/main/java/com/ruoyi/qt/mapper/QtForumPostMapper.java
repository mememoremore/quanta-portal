package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtForumPost;

public interface QtForumPostMapper
{
    List<QtForumPost> selectPostList(QtForumPost query);

    QtForumPost selectPostById(Long postId);

    QtForumPost lockPostById(Long postId);

    /** 当前数据库中的有效塔员及管理角色，不使用会话缓存角色。 */
    boolean canManagePosts(Long userId);

    List<QtForumPost> selectPersonalPostList(@Param("category") String category,
            @Param("userId") Long userId, @Param("kind") String kind);

    /**
     * 开放 API 专用：站点主人的公网可见文章（MEMBERS 或历史 null），
     * MANAGERS 限定文章永远不返回；分页由调用方 PageHelper 控制。
     */
    List<QtForumPost> selectPublicPostsByAuthor(@Param("authorId") Long authorId);

    int insertPost(QtForumPost post);

    int updatePost(QtForumPost post);

    int deletePostById(Long postId);

    int refreshCommentCount(Long postId);

    int incrementViewCount(Long postId);
}
