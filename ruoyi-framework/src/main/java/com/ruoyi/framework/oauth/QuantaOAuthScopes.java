package com.ruoyi.framework.oauth;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 本次授权只允许两组范围。客户端注册里的 email 只表示可以申请，不能当成某次 token 的实际范围。
 */
final class QuantaOAuthScopes
{
    static final String PROFILE = "openid profile";

    static final String PROFILE_EMAIL = "openid profile email";

    private QuantaOAuthScopes()
    {
    }

    static String parseRequest(String scope)
    {
        if (scope == null || scope.isEmpty())
        {
            return null;
        }
        String[] parts = scope.split(" ", -1);
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String part : parts)
        {
            if (part.isEmpty() || !names.add(part))
            {
                return null;
            }
        }
        if (!names.contains("openid") || !names.contains("profile"))
        {
            return null;
        }
        names.remove("openid");
        names.remove("profile");
        if (names.isEmpty())
        {
            return PROFILE;
        }
        if (names.size() == 1 && names.contains("email"))
        {
            return PROFILE_EMAIL;
        }
        return null;
    }

    /**
     * 缺失或无法识别的存储值保持旧的唯一合法集合，不能升级成 email。
     */
    static String granted(String stored)
    {
        return PROFILE_EMAIL.equals(stored) ? PROFILE_EMAIL : PROFILE;
    }

    static boolean includesEmail(String stored)
    {
        return PROFILE_EMAIL.equals(stored);
    }

    static Set<String> names(String canonical)
    {
        if (PROFILE_EMAIL.equals(canonical))
        {
            return Set.of("openid", "profile", "email");
        }
        return Set.of("openid", "profile");
    }

    static List<String> asList(String stored)
    {
        if (PROFILE_EMAIL.equals(stored))
        {
            return List.of("openid", "profile", "email");
        }
        return List.of("openid", "profile");
    }
}
