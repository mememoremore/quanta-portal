package com.ruoyi.qt.service;

import java.util.List;

/**
 * Quanta Pages 开放数据 API（匿名只读）。
 *
 * 按站点 slug 解析站主，只返回站主本人「塔员可见(MEMBERS)」的文章与这些文章的留言；
 * MANAGERS 限定文章永远不返回。公开资料只含社区昵称、公开头像、简介、技能与公开链接，
 * 不含真实姓名/学号/部门/userId。DTO 使用 record 做白名单映射，杜绝整实体外泄。
 */
public interface IQtPagesOpenService
{
    record PageResult<T>(List<T> items, long total, int pageNum, int pageSize)
    {
    }

    record PublicPostSummary(Long postId, String title, String category, String excerpt,
            Long commentCount, String createdAt)
    {
    }

    record PublicPostDetail(Long postId, String title, String category, String content,
            Long commentCount, String createdAt, String authorNickname)
    {
    }

    record PublicComment(Long commentId, Long parentId, String content,
            String authorNickname, String authorAvatar, String replyToNickname, String createdAt)
    {
    }

    record PublicProfile(String slug, String displayName, String avatar, String headline,
            String bio, List<String> skills, String githubUrl, String websiteUrl, String contactEmail)
    {
    }

    PageResult<PublicPostSummary> listPosts(String slug, Integer pageNum, Integer pageSize);

    PublicPostDetail getPost(String slug, Long postId);

    PageResult<PublicComment> listComments(String slug, Long postId, Integer pageNum, Integer pageSize);

    PublicProfile getProfile(String slug);
}
