package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtForumReaction;

public interface QtForumReactionMapper
{
    boolean isSchemaReady();
    List<QtForumReaction> selectStates(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);
    int insertLike(@Param("postId") Long postId, @Param("userId") Long userId);
    int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);
    int insertFavorite(@Param("postId") Long postId, @Param("userId") Long userId);
    int deleteFavorite(@Param("postId") Long postId, @Param("userId") Long userId);
    int deleteLikesByPostId(Long postId);
    int deleteFavoritesByPostId(Long postId);
}
