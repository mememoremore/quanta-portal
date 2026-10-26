package com.ruoyi.qt.service.impl;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.config.QtPagesProperties;
import com.ruoyi.qt.domain.QtForumComment;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtForumCommentMapper;
import com.ruoyi.qt.mapper.QtForumPostMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtPagesRuntime;
import com.ruoyi.qt.service.IQtPagesOpenService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QtPagesOpenServiceTest
{
    @Mock
    private QtPagesRuntime runtime;

    @Mock
    private QtPageSiteMapper siteMapper;

    @Mock
    private QtForumPostMapper postMapper;

    @Mock
    private QtForumCommentMapper commentMapper;

    @Mock
    private QtPagesProperties properties;

    @InjectMocks
    private QtPagesOpenServiceImpl service;

    private final QtPageSite site = site("demo-lab", 9L);

    @BeforeEach
    void setUp()
    {
        when(runtime.isEnabled()).thenReturn(true);
        when(siteMapper.selectBySlug("demo-lab")).thenReturn(site);
        when(properties.getAssetBaseUrl()).thenReturn("https://www.quantacenter.com");
    }

    @AfterEach
    void clearPageHelper()
    {
        PageHelper.clearPage();
    }

    @Test
    void disabledFeaturePretendsSiteIsMissing()
    {
        when(runtime.isEnabled()).thenReturn(false);
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.listPosts("demo-lab", 1, 20));
        assertEquals(HttpStatus.NOT_FOUND, exception.getCode());
        verifyNoInteractions(siteMapper, postMapper);
    }

    @Test
    void malformedOrUnknownSlugReturns404()
    {
        for (String slug : new String[] { "AB", "-x", "a", "x".repeat(64), null })
        {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.listPosts(slug, 1, 20), "slug must be rejected: " + slug);
            assertEquals(HttpStatus.NOT_FOUND, exception.getCode());
        }
        when(siteMapper.selectBySlug("ghost-lab")).thenReturn(null);
        ServiceException missing = assertThrows(ServiceException.class,
                () -> service.listPosts("ghost-lab", 1, 20));
        assertEquals(HttpStatus.NOT_FOUND, missing.getCode());
        verify(postMapper, never()).selectPublicPostsByAuthor(anyLong());
    }

    @Test
    void listPostsMapsSummariesAndClampsPageSize()
    {
        QtForumPost post = post(1L, 9L, "MEMBERS", "# 标题\n\n正文内容 [链接](https://example.com)");
        post.setCommentCount(2L);
        post.setCreateTime(new Date(1700000000000L));
        QtForumPost legacy = post(2L, 9L, null, "旧文章正文");
        when(postMapper.selectPublicPostsByAuthor(9L)).thenReturn(List.of(post, legacy));

        IQtPagesOpenService.PageResult<IQtPagesOpenService.PublicPostSummary> page =
                service.listPosts("demo-lab", null, 500);

        assertEquals(2, page.items().size());
        assertEquals(50, page.pageSize());
        assertEquals(1, page.pageNum());
        assertEquals(2L, page.total());
        assertEquals("标题 正文内容 链接", page.items().get(0).excerpt());
        assertEquals(Long.valueOf(2L), page.items().get(0).commentCount());
        assertEquals("旧文章正文", page.items().get(1).excerpt());
    }

    @Test
    void getPostRejectsForeignOrManagerOnlyPosts()
    {
        QtForumPost foreign = post(5L, 8L, "MEMBERS", "别人的文章");
        when(postMapper.selectPostById(5L)).thenReturn(foreign);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ServiceException.class, () -> service.getPost("demo-lab", 5L)).getCode());

        QtForumPost managerOnly = post(6L, 9L, "MANAGERS", "经理可见");
        when(postMapper.selectPostById(6L)).thenReturn(managerOnly);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ServiceException.class, () -> service.getPost("demo-lab", 6L)).getCode());

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ServiceException.class, () -> service.getPost("demo-lab", null)).getCode());
    }

    @Test
    void getPostReturnsMarkdownAndNicknameOnly()
    {
        QtForumPost post = post(7L, 9L, "MEMBERS", "# 我的文章\n\n**正文**");
        post.setAuthorCommunityNickname("橘猫");
        post.setAuthorName("真实姓名");
        post.setAuthorDepartment("产品部");
        post.setCommentCount(3L);
        post.setCreateTime(new Date(1700000000000L));
        when(postMapper.selectPostById(7L)).thenReturn(post);

        IQtPagesOpenService.PublicPostDetail detail = service.getPost("demo-lab", 7L);

        assertEquals("# 我的文章\n\n**正文**", detail.content());
        assertEquals("橘猫", detail.authorNickname());
        assertEquals(Long.valueOf(3L), detail.commentCount());
        assertTrue(detail.createdAt().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void commentsRequirePublicOwnedPostAndStripRealNames()
    {
        QtForumPost foreign = post(5L, 8L, "MEMBERS", "别人的文章");
        when(postMapper.selectPostById(5L)).thenReturn(foreign);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ServiceException.class,
                        () -> service.listComments("demo-lab", 5L, 1, 20)).getCode());
        verify(commentMapper, never()).selectCommentList(any());

        QtForumPost own = post(7L, 9L, "MEMBERS", "自己的文章");
        when(postMapper.selectPostById(7L)).thenReturn(own);
        QtForumComment comment = new QtForumComment();
        comment.setCommentId(11L);
        comment.setPostId(7L);
        comment.setContent("写得好");
        comment.setAuthorName("真实姓名");
        comment.setAuthorCommunityNickname("小鹿");
        comment.setAuthorAvatar("/profile/avatar/2026/10/26/abc.png");
        comment.setReplyToCommunityNickname(null);
        comment.setCreatedAt(new Date(1700000000000L));
        when(commentMapper.selectCommentList(any())).thenReturn(List.of(comment));

        IQtPagesOpenService.PageResult<IQtPagesOpenService.PublicComment> page =
                service.listComments("demo-lab", 7L, 1, 0);

        assertEquals(20, page.pageSize());
        assertEquals("写得好", page.items().get(0).content());
        assertEquals("小鹿", page.items().get(0).authorNickname());
        assertEquals("https://www.quantacenter.com/profile/avatar/2026/10/26/abc.png",
                page.items().get(0).authorAvatar());
        assertEquals("", page.items().get(0).replyToNickname());
    }

    @Test
    void commentsWithoutAvatarOrWithUnsafeAvatarReturnEmpty()
    {
        QtForumPost own = post(7L, 9L, "MEMBERS", "自己的文章");
        when(postMapper.selectPostById(7L)).thenReturn(own);
        QtForumComment missing = new QtForumComment();
        missing.setCommentId(11L);
        QtForumComment unsafe = new QtForumComment();
        unsafe.setCommentId(12L);
        unsafe.setAuthorAvatar("javascript:alert(1)");
        when(commentMapper.selectCommentList(any())).thenReturn(List.of(missing, unsafe));

        IQtPagesOpenService.PageResult<IQtPagesOpenService.PublicComment> page =
                service.listComments("demo-lab", 7L, 1, 20);

        assertEquals("", page.items().get(0).authorAvatar());
        assertEquals("", page.items().get(1).authorAvatar());
    }

    @Test
    void excerptStripsMarkdownAndTruncates()
    {
        assertEquals("", QtPagesOpenServiceImpl.excerpt(null));
        assertEquals("正文", QtPagesOpenServiceImpl.excerpt("```\ncode\n```\n正文"));
        assertEquals("标题", QtPagesOpenServiceImpl.excerpt("![图片](https://x/y.png) 标题"));
        assertTrue(QtPagesOpenServiceImpl.excerpt("字".repeat(400)).endsWith("…"));
        assertEquals(161, QtPagesOpenServiceImpl.excerpt("字".repeat(400)).length());
    }

    private static QtPageSite site(String slug, Long userId)
    {
        QtPageSite site = new QtPageSite();
        site.setSlug(slug);
        site.setUserId(userId);
        site.setStatus(QtPageSite.STATUS_ENABLED);
        return site;
    }

    private static QtForumPost post(Long postId, Long authorId, String visibility, String content)
    {
        QtForumPost post = new QtForumPost();
        post.setPostId(postId);
        post.setAuthorId(authorId);
        post.setVisibility(visibility);
        post.setContent(content);
        post.setTitle("标题");
        post.setCategory("TECH");
        return post;
    }
}
