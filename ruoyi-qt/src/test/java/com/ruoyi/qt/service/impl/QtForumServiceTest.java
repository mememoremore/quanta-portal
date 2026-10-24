package com.ruoyi.qt.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtForumComment;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.mapper.QtForumCommentMapper;
import com.ruoyi.qt.mapper.QtForumPostMapper;
import com.ruoyi.qt.mapper.QtForumReactionMapper;
import com.ruoyi.qt.domain.QtForumReaction;
import com.ruoyi.qt.service.QtForumImageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 社区论坛规则：字数/分类校验、作者编辑权、审核删除、级联与回复保留。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QtForumServiceTest
{
    @Mock
    private QtForumPostMapper postMapper;

    @Mock
    private QtForumCommentMapper commentMapper;

    @Mock
    private QtForumReactionMapper reactionMapper;
    @Mock
    private QtForumImageService imageService;

    @BeforeEach
    void defaultLock()
    {
        when(postMapper.lockPostById(any())).thenAnswer(inv -> postMapper.selectPostById(inv.getArgument(0)));
        when(reactionMapper.isSchemaReady()).thenReturn(true);
    }

    @InjectMocks
    private QtForumServiceImpl service;

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void insertPostRejectsInvalidCategoryAndLength()
    {
        login(8L, Set.of());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ServiceException.class, () -> service.insertPost(post("T", "BAD", "内容"))).getCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ServiceException.class,
                () -> service.insertPost(post("T".repeat(81), "TECH", "内容"))).getCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ServiceException.class,
                () -> service.insertPost(post("T", "TECH", "C".repeat(20001)))).getCode());
        verify(postMapper, never()).insertPost(any(QtForumPost.class));
    }

    @Test
    void insertPostUsesServerSideAuthorAndReturnsPost()
    {
        login(8L, Set.of());
        doAnswer(inv -> {
            ((QtForumPost) inv.getArgument(0)).setPostId(9L);
            return 1;
        }).when(postMapper).insertPost(any(QtForumPost.class));
        QtForumPost saved = post("T", "TECH", "内容");
        saved.setPostId(9L);
        when(postMapper.selectPostById(9L)).thenReturn(saved);

        QtForumPost result = service.insertPost(post("T", "TECH", "内容"));
        assertEquals(9L, result.getPostId());
        org.mockito.ArgumentCaptor<QtForumPost> captor = org.mockito.ArgumentCaptor.forClass(QtForumPost.class);
        verify(postMapper).insertPost(captor.capture());
        assertEquals(8L, captor.getValue().getAuthorId());
        assertEquals(0L, captor.getValue().getCommentCount() == null ? 0L : captor.getValue().getCommentCount());
    }

    @Test
    void onlyAuthorCanEditPost()
    {
        login(9L, Set.of());
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updatePost(5L, post("新标题", "TECH", "新内容")));
        assertEquals(HttpStatus.FORBIDDEN, error.getCode());

        login(8L, Set.of());
        service.updatePost(5L, post("新标题", "TECH", "新内容"));
        verify(postMapper).updatePost(any(QtForumPost.class));
    }

    @Test
    void deletePostCascadesCommentsForAuthorAndModerator()
    {
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));

        login(8L, Set.of());
        service.deletePostById(5L);
        verify(commentMapper).deleteByPostId(5L);
        verify(reactionMapper).deleteLikesByPostId(5L);
        verify(reactionMapper).deleteFavoritesByPostId(5L);
        verify(postMapper).deletePostById(5L);
    }

    @Test
    void moderatorCanDeleteButCannotEditOthersPost()
    {
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));

        login(9L, Set.of("qt:forum:moderate"));
        service.deletePostById(5L);
        verify(postMapper).deletePostById(5L);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updatePost(5L, post("改名", "TECH", "内容")));
        assertEquals(HttpStatus.FORBIDDEN, error.getCode());
    }

    @Test
    void memberCannotDeleteOthersPost()
    {
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));
        login(9L, Set.of());
        ServiceException error = assertThrows(ServiceException.class, () -> service.deletePostById(5L));
        assertEquals(HttpStatus.FORBIDDEN, error.getCode());
        verify(postMapper, never()).deletePostById(any(Long.class));
    }

    @Test
    void insertCommentRejectsCrossPostParentAndRecordsReplyName()
    {
        login(8L, Set.of());
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));

        QtForumComment otherPostParent = new QtForumComment();
        otherPostParent.setCommentId(3L);
        otherPostParent.setPostId(6L);
        when(commentMapper.selectCommentById(3L)).thenReturn(otherPostParent);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ServiceException.class,
                () -> service.insertComment(5L, comment("回复", 3L))).getCode());

        QtForumComment parent = new QtForumComment();
        parent.setCommentId(3L);
        parent.setPostId(5L);
        parent.setAuthorId(4L);
        parent.setAuthorName("甲");
        when(commentMapper.selectCommentById(3L)).thenReturn(parent);
        doAnswer(inv -> {
            ((QtForumComment) inv.getArgument(0)).setCommentId(7L);
            return 1;
        }).when(commentMapper).insertComment(any(QtForumComment.class));
        when(commentMapper.selectCommentById(7L)).thenReturn(comment("回复", 3L));

        service.insertComment(5L, comment("回复", 3L));
        org.mockito.ArgumentCaptor<QtForumComment> captor =
                org.mockito.ArgumentCaptor.forClass(QtForumComment.class);
        verify(commentMapper).insertComment(captor.capture());
        assertEquals("甲", captor.getValue().getReplyToUserName());
        assertEquals(4L, captor.getValue().getReplyToUserId());
        assertEquals(5L, captor.getValue().getPostId());
        assertEquals(8L, captor.getValue().getAuthorId());
        verify(postMapper).refreshCommentCount(5L);
    }

    @Test
    void onlyAuthorCanEditComment()
    {
        QtForumComment comment = comment("原内容", null);
        comment.setCommentId(3L);
        comment.setAuthorId(8L);
        comment.setPostId(5L);
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));
        when(commentMapper.selectCommentById(3L)).thenReturn(comment);

        login(9L, Set.of());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ServiceException.class,
                () -> service.updateComment(3L, comment("改", null))).getCode());

        login(8L, Set.of());
        service.updateComment(3L, comment("改", null));
        verify(commentMapper).updateCommentContent(any(QtForumComment.class));
    }

    @Test
    void deleteCommentKeepsRepliesAndRefreshesCount()
    {
        QtForumComment comment = comment("原内容", null);
        comment.setCommentId(3L);
        comment.setPostId(5L);
        comment.setAuthorId(8L);
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));
        when(commentMapper.selectCommentById(3L)).thenReturn(comment);

        login(8L, Set.of());
        service.deleteCommentById(3L);
        verify(commentMapper).promoteReplies(comment);
        verify(commentMapper).deleteCommentById(3L);
        verify(postMapper).refreshCommentCount(5L);
    }

    @Test
    void viewPostIncrementsViewCountOnce()
    {
        login(8L, Set.of());
        QtForumPost first = postWithAuthor(5L, 8L);
        first.setViewCount(2L);
        QtForumPost updated = postWithAuthor(5L, 8L);
        updated.setViewCount(3L);
        when(postMapper.selectPostById(5L)).thenReturn(first, updated);
        when(postMapper.incrementViewCount(5L)).thenReturn(1);
        QtForumPost result = service.viewPost(5L);
        assertEquals(3L, result.getViewCount());
        verify(postMapper).incrementViewCount(5L);
    }

    @Test
    void selectPostListByAuthorUsesServerSideAuthorId()
    {
        login(8L, Set.of());
        when(postMapper.selectPostList(org.mockito.ArgumentMatchers.argThat(query -> query.getAuthorId() == 12L)))
                .thenReturn(List.of(postWithAuthor(1L, 12L)));
        assertEquals(1, service.selectPostListByAuthor(12L).size());
    }

    @Test
    void commentLengthIsValidated()
    {
        login(8L, Set.of());
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ServiceException.class, () -> service.insertComment(5L, comment("", null))).getCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ServiceException.class,
                () -> service.insertComment(5L, comment("C".repeat(1001), null))).getCode());
        verify(commentMapper, never()).insertComment(any(QtForumComment.class));
    }

    @Test
    void reactionsLockPostAndAlwaysUseTheSessionUser()
    {
        login(8L, Set.of());
        org.mockito.Mockito.doReturn(postWithAuthor(5L, 8L)).when(postMapper).lockPostById(5L);
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));
        QtForumReaction state = new QtForumReaction();
        state.setPostId(5L);
        state.setLikeCount(1);
        state.setLiked(true);
        when(reactionMapper.selectStates(List.of(5L), 8L)).thenReturn(List.of(state));
        assertEquals(1, service.setReaction(5L, "liked", true).getLikeCount());
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(postMapper, reactionMapper);
        order.verify(postMapper).lockPostById(5L);
        order.verify(reactionMapper).insertLike(5L, 8L);
        order.verify(reactionMapper).selectStates(List.of(5L), 8L);
        service.setReaction(5L, "liked", false);
        verify(reactionMapper).deleteLike(5L, 8L);
        service.setReaction(5L, "favorites", true);
        service.setReaction(5L, "favorites", false);
        verify(reactionMapper).insertFavorite(5L, 8L);
        verify(reactionMapper).deleteFavorite(5L, 8L);
        verify(postMapper, never()).incrementViewCount(any());
    }

    @Test
    void reactionValidationRejectsNonMemberInvalidOrDeletedPost()
    {
        login(8L, Set.of());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ServiceException.class,
                () -> service.setReaction(0L, "liked", true)).getCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ServiceException.class,
                () -> service.setReaction(1L, "arbitrary", true)).getCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ServiceException.class,
                () -> service.setReaction(99L, "liked", true)).getCode());
        verify(reactionMapper, never()).insertLike(any(), any());
        setNonMember();
        assertThrows(ServiceException.class, () -> service.setReaction(5L, "liked", true));
    }

    private void setNonMember()
    {
        LoginUser current = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        current.getUser().setIsQuantaMember("0");
        current.getUser().setRoles(List.of());
    }

    @Test
    void privateHistoryIgnoresClientAuthorAndBatchEnrichesThePage()
    {
        login(8L, Set.of());
        QtForumPost query = new QtForumPost();
        query.setAuthorId(999L);
        query.setCategory("TECH");
        when(postMapper.selectPersonalPostList("TECH", 8L, "liked"))
                .thenReturn(List.of(postWithAuthor(5L, 9L), postWithAuthor(6L, 10L)));
        assertEquals(2, service.selectPersonalPostList(query, "liked").size());
        verify(reactionMapper).selectStates(List.of(5L, 6L), 8L);
        verify(postMapper).selectPersonalPostList("TECH", 8L, "liked");
    }

    @Test
    void missingMigrationDoesNotBreakReadingOrDeletingExistingPosts()
    {
        login(8L, Set.of());
        when(reactionMapper.isSchemaReady()).thenReturn(false);
        when(postMapper.selectPostById(5L)).thenReturn(postWithAuthor(5L, 8L));
        assertEquals(false, service.selectPostById(5L).isReactionsReady());
        service.deletePostById(5L);
        verify(reactionMapper, never()).deleteLikesByPostId(any());
        verify(reactionMapper, never()).selectStates(any(), any());
        assertEquals(503, assertThrows(ServiceException.class,
                () -> service.selectPersonalPostList(new QtForumPost(), "liked")).getCode());
    }

    @Test
    void privatePostCannotBeReadEvenByItsDemotedAuthor()
    {
        login(8L, Set.of("*:*:*"));
        QtForumPost restricted = postWithAuthor(5L, 8L);
        restricted.setVisibility("MANAGERS");
        when(postMapper.selectPostById(5L)).thenReturn(restricted);
        assertEquals(404, assertThrows(ServiceException.class, () -> service.selectPostById(5L)).getCode());
        assertThrows(ServiceException.class, () -> service.viewPost(5L));
        assertThrows(ServiceException.class, () -> service.selectCommentList(5L));
        assertThrows(ServiceException.class, () -> service.setReaction(5L, "liked", true));
        verify(reactionMapper, never()).insertLike(any(), any());
        verify(postMapper, never()).incrementViewCount(any());
    }

    @Test
    void clientCannotChoosePrivateScopeWithoutCurrentManagerRole()
    {
        login(8L, Set.of());
        QtForumPost body = post("标题", "TECH", "内容");
        body.setVisibility("MANAGERS");
        assertEquals(403, assertThrows(ServiceException.class, () -> service.insertPost(body)).getCode());
        body.setVisibility("ARBITRARY");
        assertEquals(400, assertThrows(ServiceException.class, () -> service.insertPost(body)).getCode());
        verify(postMapper, never()).insertPost(any());
    }

    @Test
    void forgedViewerIdIsOverwrittenBeforeQuery()
    {
        login(8L, Set.of());
        QtForumPost query = new QtForumPost();
        query.setViewerId(1L);
        service.selectPostList(query, false);
        assertEquals(8L, query.getViewerId());
    }

    @Test
    void managerScopeIsRecheckedAndOldUpdateCannotAccidentallyPublishIt()
    {
        login(8L, Set.of());
        QtForumPost existing = postWithAuthor(5L, 8L);
        existing.setVisibility("MANAGERS");
        when(postMapper.selectPostById(5L)).thenReturn(existing);
        when(postMapper.canManagePosts(8L)).thenReturn(true);
        service.updatePost(5L, post("新标题", "TECH", "内容"));
        assertEquals("MANAGERS", existing.getVisibility());
        when(postMapper.canManagePosts(8L)).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.getReactions(5L));
        QtForumComment comment = comment("不可更新", null);
        comment.setPostId(5L); comment.setAuthorId(8L);
        when(commentMapper.selectCommentById(3L)).thenReturn(comment);
        assertThrows(ServiceException.class, () -> service.updateComment(3L, comment));
        assertThrows(ServiceException.class, () -> service.deleteCommentById(3L));
    }

    private QtForumPost post(String title, String category, String content)
    {
        QtForumPost post = new QtForumPost();
        post.setTitle(title);
        post.setCategory(category);
        post.setContent(content);
        return post;
    }

    private QtForumPost postWithAuthor(Long postId, Long authorId)
    {
        QtForumPost post = post("标题", "TECH", "内容");
        post.setPostId(postId);
        post.setAuthorId(authorId);
        return post;
    }

    private QtForumComment comment(String content, Long parentId)
    {
        QtForumComment comment = new QtForumComment();
        comment.setContent(content);
        comment.setParentId(parentId);
        return comment;
    }

    private void login(Long userId, Set<String> permissions)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setIsQuantaMember("1");
        SysRole role = new SysRole();
        role.setRoleKey("qt_member");
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>(permissions));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
