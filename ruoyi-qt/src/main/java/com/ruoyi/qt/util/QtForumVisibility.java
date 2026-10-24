package com.ruoyi.qt.util;

import java.util.function.BooleanSupplier;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtForumPost;

/** 所有按ID读取（含图片）使用同一失败关闭规则，不以作者身份绕过范围。 */
public final class QtForumVisibility
{
    public static final String MEMBERS = "MEMBERS";
    public static final String MANAGERS = "MANAGERS";

    private QtForumVisibility() { }

    public static void requireReadable(QtForumPost post, BooleanSupplier currentManager)
    {
        if (post == null || (!MEMBERS.equals(post.getVisibility())
                && post.getVisibility() != null
                && !(MANAGERS.equals(post.getVisibility()) && currentManager.getAsBoolean())))
            throw new ServiceException("帖子不存在或已删除", 404);
    }
}
