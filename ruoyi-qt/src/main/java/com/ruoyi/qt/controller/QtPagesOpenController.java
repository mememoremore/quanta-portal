package com.ruoyi.qt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.qt.service.IQtPagesOpenService;

/**
 * Quanta Pages 开放数据 API（匿名只读）。
 *
 * 供成员个人静态站点（{slug}.page.quantacenter.com）的页面 JS 跨域读取：
 * 站主本人「塔员可见」的文章与这些文章的留言。只暴露公开字段（评论仅社区昵称与公开头像），
 * MANAGERS 限定文章永远不返回；错误由 QtPagesOpenExceptionHandler 映射为真实 HTTP 状态。
 * CORS 仅放行 Pages 子域且不携带凭据（见 ResourcesConfig）。
 */
@Anonymous
@RestController
@RequestMapping("/open/pages")
public class QtPagesOpenController
{
    private static final String RATE_LIMIT_KEY = "rate_limit:open_pages:";

    private static final int RATE_LIMIT_SECONDS = 60;

    private static final int RATE_LIMIT_COUNT = 120;

    @Autowired
    private IQtPagesOpenService openService;

    /**
     * 站主公开文章列表（摘要，分页）。
     */
    @RateLimiter(time = RATE_LIMIT_SECONDS, count = RATE_LIMIT_COUNT, limitType = LimitType.IP, key = RATE_LIMIT_KEY)
    @GetMapping("/{slug}/posts")
    public AjaxResult posts(@PathVariable("slug") String slug,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize)
    {
        return AjaxResult.success(openService.listPosts(slug, pageNum, pageSize));
    }

    /**
     * 文章详情（Markdown 原文）。
     */
    @RateLimiter(time = RATE_LIMIT_SECONDS, count = RATE_LIMIT_COUNT, limitType = LimitType.IP, key = RATE_LIMIT_KEY)
    @GetMapping("/{slug}/posts/{postId:\\d+}")
    public AjaxResult post(@PathVariable("slug") String slug, @PathVariable("postId") Long postId)
    {
        return AjaxResult.success(openService.getPost(slug, postId));
    }

    /**
     * 文章留言（社区昵称 + 公开头像，分页）。
     */
    @RateLimiter(time = RATE_LIMIT_SECONDS, count = RATE_LIMIT_COUNT, limitType = LimitType.IP, key = RATE_LIMIT_KEY)
    @GetMapping("/{slug}/posts/{postId:\\d+}/comments")
    public AjaxResult comments(@PathVariable("slug") String slug, @PathVariable("postId") Long postId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize)
    {
        return AjaxResult.success(openService.listComments(slug, postId, pageNum, pageSize));
    }

    /**
     * 站主公开资料（社区昵称、头像、简介、技能与公开链接）。
     */
    @RateLimiter(time = RATE_LIMIT_SECONDS, count = RATE_LIMIT_COUNT, limitType = LimitType.IP, key = RATE_LIMIT_KEY)
    @GetMapping("/{slug}/profile")
    public AjaxResult profile(@PathVariable("slug") String slug)
    {
        return AjaxResult.success(openService.getProfile(slug));
    }
}
