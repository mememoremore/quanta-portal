package com.ruoyi.qt.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtForumComment;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.domain.QtForumReaction;
import com.ruoyi.qt.mapper.QtForumReactionMapper;
import com.ruoyi.qt.mapper.QtForumCommentMapper;
import com.ruoyi.qt.mapper.QtForumPostMapper;
import com.ruoyi.qt.service.IQtForumService;
import com.ruoyi.qt.util.QtAuthUtils;

/**
 * 社区论坛：所有身份字段（作者 ID/姓名/部门）均由服务端确定，不信任客户端传参。
 */
@Service
public class QtForumServiceImpl implements IQtForumService
{
    private static final int TITLE_MAX = 80;
    private static final int CONTENT_MAX = 20000;
    private static final int COMMENT_MAX = 1000;

    @Autowired
    private QtForumPostMapper postMapper;

    @Autowired
    private QtForumCommentMapper commentMapper;

    @Autowired
    private QtForumReactionMapper reactionMapper;
    private volatile boolean reactionsSchemaReady;

    private boolean reactionsAvailable()
    {
        if (!reactionsSchemaReady) reactionsSchemaReady = reactionMapper.isSchemaReady();
        return reactionsSchemaReady;
    }

    private void requireReactionsAvailable()
    {
        if (!reactionsAvailable()) throw new ServiceException("论坛互动尚未开放，请稍后重试", 503);
    }

    @Override
    public void checkReactionsAvailable()
    {
        QtAuthUtils.requireQuantaMember();
        requireReactionsAvailable();
    }

    @Override
    public List<QtForumPost> selectPostList(QtForumPost query, boolean mine)
    {
        // 作者过滤只认服务端登录身份，忽略客户端传入的 authorId
        query.setAuthorId(mine ? SecurityUtils.getUserId() : null);
        return withReactions(postMapper.selectPostList(query));
    }

    @Override
    public List<QtForumPost> selectPostListByAuthor(Long authorId)
    {
        QtForumPost query = new QtForumPost();
        query.setAuthorId(authorId);
        return withReactions(postMapper.selectPostList(query));
    }

    @Override
    public List<QtForumPost> selectPersonalPostList(QtForumPost query, String kind)
    {
        QtAuthUtils.requireQuantaMember();
        requireKind(kind);
        // 不使用客户端的authorId/userId/params，只接受分类及分页。
        requireReactionsAvailable();
        return withReactions(postMapper.selectPersonalPostList(query.getCategory(), SecurityUtils.getUserId(), kind));
    }

    private List<QtForumPost> withReactions(List<QtForumPost> posts)
    {
        if (posts == null || posts.isEmpty()) return posts;
        if (!reactionsAvailable()) return posts;
        Map<Long, QtForumReaction> states = reactionMapper.selectStates(
                posts.stream().map(QtForumPost::getPostId).collect(Collectors.toList()), SecurityUtils.getUserId())
                .stream().collect(Collectors.toMap(QtForumReaction::getPostId, Function.identity()));
        for (QtForumPost post : posts)
        {
            QtForumReaction state = states.get(post.getPostId());
            if (state != null)
            {
                post.setLikeCount(state.getLikeCount());
                post.setLiked(state.isLiked());
                post.setFavorited(state.isFavorited());
                post.setReactionsReady(true);
            }
        }
        return posts;
    }

    @Override
    public QtForumReaction getReactions(Long postId)
    {
        QtAuthUtils.requireQuantaMember();
        requirePostId(postId);
        requireReactionsAvailable();
        List<QtForumReaction> states = reactionMapper.selectStates(List.of(postId), SecurityUtils.getUserId());
        if (states.isEmpty()) throw new ServiceException("帖子不存在或已删除", HttpStatus.NOT_FOUND);
        return states.get(0);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QtForumReaction setReaction(Long postId, String kind, boolean active)
    {
        QtAuthUtils.requireQuantaMember();
        requireKind(kind);
        requireReactionsAvailable();
        lockPost(postId);
        Long userId = SecurityUtils.getUserId();
        if ("liked".equals(kind))
        {
            if (active) reactionMapper.insertLike(postId, userId);
            else reactionMapper.deleteLike(postId, userId);
        }
        else
        {
            if (active) reactionMapper.insertFavorite(postId, userId);
            else reactionMapper.deleteFavorite(postId, userId);
        }
        return getReactions(postId);
    }

    private void requireKind(String kind)
    {
        if (!"liked".equals(kind) && !"favorites".equals(kind))
            throw new ServiceException("不支持的互动类型", HttpStatus.BAD_REQUEST);
    }

    private void requirePostId(Long postId)
    {
        if (postId == null || postId <= 0) throw new ServiceException("无效的帖子编号", HttpStatus.BAD_REQUEST);
    }

    private QtForumPost lockPost(Long postId)
    {
        requirePostId(postId);
        QtForumPost post = postMapper.lockPostById(postId);
        if (post == null) throw new ServiceException("帖子不存在或已删除", HttpStatus.NOT_FOUND);
        return post;
    }

    @Override
    public QtForumPost selectPostById(Long postId)
    {
        QtForumPost post = postMapper.selectPostById(postId);
        if (post == null)
        {
            throw new ServiceException("帖子不存在或已删除", HttpStatus.NOT_FOUND);
        }
        withReactions(List.of(post));
        return post;
    }

    @Override
    @Transactional
    public QtForumPost viewPost(Long postId)
    {
        selectPostById(postId);
        if (postMapper.incrementViewCount(postId) == 0)
        {
            throw new ServiceException("帖子不存在或已删除", HttpStatus.NOT_FOUND);
        }
        return selectPostById(postId);
    }

    @Override
    public QtForumPost insertPost(QtForumPost post)
    {
        validatePost(post);
        post.setPostId(null);
        post.setAuthorId(SecurityUtils.getUserId());
        postMapper.insertPost(post);
        return selectPostById(post.getPostId());
    }

    @Override
    public QtForumPost updatePost(Long postId, QtForumPost body)
    {
        QtForumPost post = selectPostById(postId);
        if (!SecurityUtils.getUserId().equals(post.getAuthorId()))
        {
            throw new ServiceException("只能编辑自己发布的帖子", HttpStatus.FORBIDDEN);
        }
        validatePost(body);
        post.setTitle(body.getTitle());
        post.setCategory(body.getCategory());
        post.setContent(body.getContent());
        postMapper.updatePost(post);
        return selectPostById(postId);
    }

    @Override
    @Transactional
    public void deletePostById(Long postId)
    {
        QtForumPost post = lockPost(postId);
        if (!canModerate(post.getAuthorId()))
        {
            throw new ServiceException("只能删除自己发布的帖子", HttpStatus.FORBIDDEN);
        }
        // 与互动写入持相同帖子行锁，先清关系再删帖子。
        if (reactionsAvailable())
        {
            reactionMapper.deleteLikesByPostId(postId);
            reactionMapper.deleteFavoritesByPostId(postId);
        }
        commentMapper.deleteByPostId(postId);
        postMapper.deletePostById(postId);
    }

    @Override
    public List<QtForumComment> selectCommentList(Long postId)
    {
        selectPostById(postId);
        QtForumComment query = new QtForumComment();
        query.setPostId(postId);
        return commentMapper.selectCommentList(query);
    }

    @Override
    @Transactional
    public QtForumComment insertComment(Long postId, QtForumComment body)
    {
        selectPostById(postId);
        String content = validateCommentContent(body);
        Long parentId = body.getParentId() == null ? null : body.getParentId();
        String replyToUserName = null;
        Long replyToUserId = null;
        if (parentId != null)
        {
            QtForumComment parent = commentMapper.selectCommentById(parentId);
            if (parent == null || !postId.equals(parent.getPostId()))
            {
                throw new ServiceException("回复目标不存在", HttpStatus.BAD_REQUEST);
            }
            replyToUserName = parent.getAuthorName();
            replyToUserId = parent.getAuthorId();
        }
        QtForumComment comment = new QtForumComment();
        comment.setPostId(postId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setAuthorId(SecurityUtils.getUserId());
        comment.setReplyToUserId(replyToUserId);
        comment.setReplyToUserName(replyToUserName);
        commentMapper.insertComment(comment);
        postMapper.refreshCommentCount(postId);
        return commentMapper.selectCommentById(comment.getCommentId());
    }

    @Override
    @Transactional
    public QtForumComment updateComment(Long commentId, QtForumComment body)
    {
        QtForumComment comment = requireComment(commentId);
        if (!SecurityUtils.getUserId().equals(comment.getAuthorId()))
        {
            throw new ServiceException("只能编辑自己发布的评论", HttpStatus.FORBIDDEN);
        }
        comment.setContent(validateCommentContent(body));
        commentMapper.updateCommentContent(comment);
        return commentMapper.selectCommentById(commentId);
    }

    @Override
    @Transactional
    public void deleteCommentById(Long commentId)
    {
        QtForumComment comment = requireComment(commentId);
        if (!canModerate(comment.getAuthorId()))
        {
            throw new ServiceException("只能删除自己发布的评论", HttpStatus.FORBIDDEN);
        }
        // 删除评论保留回复：直接子回复提升为顶层，reply_to_user_name 快照仍指向被回复人
        commentMapper.promoteReplies(comment);
        commentMapper.deleteCommentById(commentId);
        postMapper.refreshCommentCount(comment.getPostId());
    }

    @Override
    public boolean canModerate(Long authorId)
    {
        Long current = SecurityUtils.getUserId();
        if (current != null && current.equals(authorId))
        {
            return true;
        }
        // SecurityUtils.hasPermi 对 *:*:*（超管）返回 true
        return QtAuthUtils.hasPermi("qt:forum:moderate");
    }

    private QtForumComment requireComment(Long commentId)
    {
        QtForumComment comment = commentMapper.selectCommentById(commentId);
        if (comment == null)
        {
            throw new ServiceException("评论不存在或已删除", HttpStatus.NOT_FOUND);
        }
        return comment;
    }

    private void validatePost(QtForumPost post)
    {
        String title = post.getTitle() == null ? "" : post.getTitle().trim();
        String content = post.getContent() == null ? "" : post.getContent().trim();
        if (StringUtils.isEmpty(title) || title.length() > TITLE_MAX)
        {
            throw new ServiceException("标题需为 1-80 字", HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isEmpty(content) || content.length() > CONTENT_MAX)
        {
            throw new ServiceException("正文需为 1-20000 字", HttpStatus.BAD_REQUEST);
        }
        if (!QtForumPost.CATEGORIES.contains(post.getCategory()))
        {
            throw new ServiceException("请选择帖子分类", HttpStatus.BAD_REQUEST);
        }
        post.setTitle(title);
        post.setContent(content);
    }

    private String validateCommentContent(QtForumComment body)
    {
        String content = body.getContent() == null ? "" : body.getContent().trim();
        if (StringUtils.isEmpty(content) || content.length() > COMMENT_MAX)
        {
            throw new ServiceException("评论需为 1-1000 字", HttpStatus.BAD_REQUEST);
        }
        return content;
    }
}
