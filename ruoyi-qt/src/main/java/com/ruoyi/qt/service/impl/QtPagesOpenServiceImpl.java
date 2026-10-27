package com.ruoyi.qt.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.config.QtPagesProperties;
import com.ruoyi.qt.domain.QtForumComment;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.domain.QtPagesPublicProfile;
import com.ruoyi.qt.mapper.QtForumCommentMapper;
import com.ruoyi.qt.mapper.QtForumPostMapper;
import com.ruoyi.qt.mapper.QtMemberProfileMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtPagesRuntime;
import com.ruoyi.qt.service.IQtPagesOpenService;
import com.ruoyi.qt.util.QtPagesAssetUrl;

/**
 * 开放数据 API 实现：匿名、只读、按站点 slug 授权。
 *
 * 不调用任何需要登录态的工具类；不递增论坛浏览量；留言只暴露社区昵称与
 * 门户域名下的公开头像 URL；所有对不存在/越权/不可公开资源的访问统一返回 404，
 * 避免暴露站点与文章的存在性。
 */
@Service
public class QtPagesOpenServiceImpl implements IQtPagesOpenService
{
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 50;

    private static final int EXCERPT_LIMIT = 160;

    private static final String VISIBILITY_MANAGERS = "MANAGERS";

    @Autowired
    private QtPagesRuntime runtime;

    @Autowired
    private QtPageSiteMapper siteMapper;

    @Autowired
    private QtForumPostMapper postMapper;

    @Autowired
    private QtForumCommentMapper commentMapper;

    @Autowired
    private QtMemberProfileMapper memberProfileMapper;

    @Autowired
    private QtPagesProperties properties;

    @Override
    public PageResult<PublicPostSummary> listPosts(String slug, Integer pageNum, Integer pageSize)
    {
        QtPageSite site = requireSite(slug);
        int number = normalizePageNum(pageNum);
        int size = normalizePageSize(pageSize);
        PageHelper.startPage(number, size);
        List<QtForumPost> posts = postMapper.selectPublicPostsByAuthor(site.getUserId());
        List<PublicPostSummary> items = posts.stream().map(this::toSummary).toList();
        return new PageResult<>(items, totalOf(posts, items.size()), number, size);
    }

    @Override
    public PublicPostDetail getPost(String slug, Long postId)
    {
        QtPageSite site = requireSite(slug);
        QtForumPost post = requirePublicOwnedPost(site, postId);
        return new PublicPostDetail(post.getPostId(), post.getTitle(), post.getCategory(), post.getContent(),
                post.getCommentCount(), formatTime(post.getCreateTime()),
                nullToEmpty(post.getAuthorCommunityNickname()));
    }

    @Override
    public PageResult<PublicComment> listComments(String slug, Long postId, Integer pageNum, Integer pageSize)
    {
        QtPageSite site = requireSite(slug);
        requirePublicOwnedPost(site, postId);
        int number = normalizePageNum(pageNum);
        int size = normalizePageSize(pageSize);
        PageHelper.startPage(number, size);
        QtForumComment query = new QtForumComment();
        query.setPostId(postId);
        List<QtForumComment> comments = commentMapper.selectCommentList(query);
        List<PublicComment> items = comments.stream().map(this::toPublicComment).toList();
        return new PageResult<>(items, totalOf(comments, items.size()), number, size);
    }

    @Override
    public PublicProfile getProfile(String slug)
    {
        QtPageSite site = requireSite(slug);
        QtPagesPublicProfile profile = memberProfileMapper.selectPublicProfileByUserId(site.getUserId());
        if (profile == null)
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        return new PublicProfile(site.getSlug(),
                nullToEmpty(profile.getCommunityNickname()),
                QtPagesAssetUrl.toPublicUrl(profile.getAvatar(), properties.getAssetBaseUrl()),
                nullToEmpty(profile.getHeadline()),
                nullToEmpty(profile.getBio()),
                parseSkills(profile.getSkills()),
                nullToEmpty(profile.getGithubUrl()),
                nullToEmpty(profile.getWebsiteUrl()),
                publicContactEmail(profile));
    }

    /**
     * 只展示成员主动填写的技能标签，最多 5 个；脏数据不影响资料接口。
     */
    static List<String> parseSkills(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return List.of();
        }
        try
        {
            List<String> skills = JSON.parseArray(raw, String.class);
            if (skills == null)
            {
                return List.of();
            }
            return skills.stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .limit(5)
                    .toList();
        }
        catch (RuntimeException e)
        {
            return List.of();
        }
    }

    /**
     * 联系邮箱必须同时满足「已填写」与「本人勾选公开」；两者缺一都返回空串。
     */
    static String publicContactEmail(QtPagesPublicProfile profile)
    {
        if (!"1".equals(profile.getShowContactEmail()) || profile.getContactEmail() == null)
        {
            return "";
        }
        return profile.getContactEmail().trim();
    }

    /**
     * 解析站点并做功能门禁：功能关闭、slug 非法、站点不存在一律 404（不暴露内部状态）。
     */
    private QtPageSite requireSite(String slug)
    {
        if (!runtime.isEnabled())
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        if (slug == null || !SLUG_PATTERN.matcher(slug).matches())
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        QtPageSite site = siteMapper.selectBySlug(slug);
        if (site == null || site.getUserId() == null)
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        return site;
    }

    /**
     * 文章必须属于站主且公网可见（MEMBERS 或历史 null）；否则 404。
     */
    private QtForumPost requirePublicOwnedPost(QtPageSite site, Long postId)
    {
        QtForumPost post = postId == null ? null : postMapper.selectPostById(postId);
        if (post == null || !site.getUserId().equals(post.getAuthorId())
                || VISIBILITY_MANAGERS.equals(post.getVisibility()))
        {
            throw new ServiceException("文章不存在或未公开", HttpStatus.NOT_FOUND);
        }
        return post;
    }

    private PublicPostSummary toSummary(QtForumPost post)
    {
        return new PublicPostSummary(post.getPostId(), post.getTitle(), post.getCategory(),
                excerpt(post.getContent()), post.getCommentCount(), formatTime(post.getCreateTime()));
    }

    private PublicComment toPublicComment(QtForumComment comment)
    {
        return new PublicComment(comment.getCommentId(), comment.getParentId(), comment.getContent(),
                nullToEmpty(comment.getAuthorCommunityNickname()),
                QtPagesAssetUrl.toPublicUrl(comment.getAuthorAvatar(), properties.getAssetBaseUrl()),
                nullToEmpty(comment.getReplyToCommunityNickname()), formatTime(comment.getCreatedAt()));
    }

    private static long totalOf(List<?> rows, int fallbackSize)
    {
        return rows instanceof Page<?> page ? page.getTotal() : fallbackSize;
    }

    private static int normalizePageNum(Integer pageNum)
    {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private static int normalizePageSize(Integer pageSize)
    {
        if (pageSize == null || pageSize < 1)
        {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /** Markdown 摘要：剥离代码块/图片/链接语法后的纯文本前 160 字。 */
    static String excerpt(String content)
    {
        if (content == null || content.isBlank())
        {
            return "";
        }
        String plain = content
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ")
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("[#>*_`~|\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (plain.length() <= EXCERPT_LIMIT)
        {
            return plain;
        }
        return plain.substring(0, EXCERPT_LIMIT) + "…";
    }

    private static String formatTime(Date date)
    {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }
}
