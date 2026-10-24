package com.ruoyi.qt.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtForumImage;

public interface QtForumImageMapper
{
    QtForumImage selectImage(String imageId);
    QtForumImage lockImage(String imageId);
    int insertImage(QtForumImage image);
    int bindImage(@Param("imageId") String imageId, @Param("postId") Long postId);
}
