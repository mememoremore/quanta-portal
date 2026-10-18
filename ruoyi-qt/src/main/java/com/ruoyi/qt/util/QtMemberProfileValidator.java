package com.ruoyi.qt.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtMemberProfileUpdateBody;

public final class QtMemberProfileValidator
{
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private QtMemberProfileValidator()
    {
    }

    public static QtMemberProfileUpdateBody normalize(QtMemberProfileUpdateBody body)
    {
        QtMemberProfileUpdateBody normalized = new QtMemberProfileUpdateBody();
        normalized.setCommunityNickname(trim(body.getCommunityNickname()));
        normalized.setHeadline(trim(body.getHeadline()));
        normalized.setBio(trim(body.getBio()));
        normalized.setSkills(normalizeSkills(body.getSkills()));
        normalized.setGithubUrl(trim(body.getGithubUrl()));
        normalized.setWebsiteUrl(trim(body.getWebsiteUrl()));
        normalized.setContactEmail(trim(body.getContactEmail()));
        normalized.setShowContactEmail(Boolean.TRUE.equals(body.getShowContactEmail()));
        return normalized;
    }

    public static void validate(QtMemberProfileUpdateBody body)
    {
        QtMemberProfileUpdateBody data = normalize(body);
        if (data.getCommunityNickname().length() > 20 || hasControlCharacter(data.getCommunityNickname()))
        {
            throw badRequest("社区昵称最多 20 字，不能包含换行或控制字符");
        }
        if (data.getHeadline().length() > 80)
        {
            throw badRequest("一句话介绍最多 80 字");
        }
        if (data.getBio().length() > 300)
        {
            throw badRequest("个人简介最多 300 字");
        }
        if (data.getSkills().size() > 5 || data.getSkills().stream().anyMatch(skill -> skill.length() > 20))
        {
            throw badRequest("最多 5 个技能标签，每个不超过 20 字");
        }
        for (String url : List.of(data.getGithubUrl(), data.getWebsiteUrl()))
        {
            if (StringUtils.isNotEmpty(url) && (url.length() > 300 || !isPublicHttpUrl(url)))
            {
                throw badRequest("个人链接需为有效的 http 或 https 地址，最多 300 字符");
            }
        }
        if (StringUtils.isNotEmpty(data.getContactEmail())
                && (data.getContactEmail().length() > 160 || !EMAIL.matcher(data.getContactEmail()).matches()))
        {
            throw badRequest("请输入正确的联系邮箱");
        }
        if (data.getShowContactEmail() && StringUtils.isEmpty(data.getContactEmail()))
        {
            throw badRequest("公开联系邮箱前请先填写邮箱");
        }
    }

    private static List<String> normalizeSkills(List<String> skills)
    {
        Set<String> unique = new LinkedHashSet<>();
        if (skills != null)
        {
            for (String skill : skills)
            {
                String value = trim(skill);
                if (StringUtils.isNotEmpty(value))
                {
                    unique.add(value);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static boolean isPublicHttpUrl(String value)
    {
        try
        {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null)
            {
                return false;
            }
            String lower = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(lower) && !"https".equals(lower))
            {
                return false;
            }
            return StringUtils.isEmpty(uri.getUserInfo());
        }
        catch (URISyntaxException ex)
        {
            return false;
        }
    }

    private static boolean hasControlCharacter(String value)
    {
        for (int i = 0; i < value.length(); i++)
        {
            char current = value.charAt(i);
            if (current < 32 || current == 127)
            {
                return true;
            }
        }
        return false;
    }

    private static String trim(String value)
    {
        return value == null ? "" : value.trim();
    }

    private static ServiceException badRequest(String message)
    {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }
}
