package com.ruoyi.qt.util;

/**
 * Pages 开放 API 的公开资源 URL 归一化。
 *
 * 头像在库中存的是 /profile/avatar/... 相对路径（历史上也可能是同源绝对 URL），
 * 而成员站点与门户不同源，必须返回绝对 URL 才能被 img 直接加载。
 * 只放行门户头像目录下的常见图片，避免把任意外链或脚本协议带进匿名接口。
 */
public final class QtPagesAssetUrl
{
    private static final String AVATAR_PATH_PATTERN =
            "/profile/avatar/[A-Za-z0-9][A-Za-z0-9._/-]*\\.(?i)(jpg|jpeg|png|webp)";

    private QtPagesAssetUrl()
    {
    }

    /**
     * 把库中头像值转成可直接访问的绝对 URL；空值或不可信值返回空串。
     */
    public static String toPublicUrl(String raw, String base)
    {
        if (raw == null || base == null)
        {
            return "";
        }
        String value = raw.trim();
        String origin = base.trim().replaceAll("/+$", "");
        if (value.isEmpty() || origin.isEmpty() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\0') >= 0)
        {
            return "";
        }
        String path = value;
        if (value.regionMatches(true, 0, "https://", 0, "https://".length())
                || value.regionMatches(true, 0, "http://", 0, "http://".length()))
        {
            String prefix = origin + "/";
            if (!value.startsWith(prefix))
            {
                return "";
            }
            path = value.substring(origin.length());
        }
        else if (value.contains(":") || value.startsWith("//"))
        {
            return "";
        }
        if (path.indexOf('?') >= 0 || path.indexOf('#') >= 0 || path.indexOf('\\') >= 0 || path.indexOf('@') >= 0
                || path.indexOf('%') >= 0 || path.contains("..") || path.contains("//")
                || !path.matches(AVATAR_PATH_PATTERN))
        {
            return "";
        }
        return origin + path;
    }
}
