package com.ruoyi.qt.cache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.util.QtAuthUtils;

/**
 * 查询缓存指纹。
 * <p>
 * 指纹覆盖「分页参数 + 查询对象的全部非空字段」，用反射收集，避免手工枚举：新增一个查询条件时
 * 若忘了加进 key，两个不同结果集会撞同一个键，属于最难排查的一类 bug。
 *
 * @author Quanta
 */
public final class QtCacheKeys
{
    private QtCacheKeys()
    {
    }

    /**
     * 当前请求的分页参数指纹。
     */
    public static String page()
    {
        PageDomain domain = TableSupport.buildPageRequest();
        int pageNum = domain.getPageNum() == null ? 1 : domain.getPageNum();
        int pageSize = domain.getPageSize() == null ? 10 : domain.getPageSize();
        return "p" + pageNum + "s" + pageSize + StringUtils.nvl(domain.getOrderBy(), "");
    }

    /**
     * 分页列表指纹：页码 + 查询条件。
     *
     * @param query 控制器收到的查询对象
     */
    public static String list(Object query)
    {
        return page() + "|" + hash(fieldsOf(query));
    }

    /**
     * 显式取值指纹，用于非实体入参（按 ID、按数据权限维度等）。
     */
    public static String of(Object... conditions)
    {
        StringBuilder sb = new StringBuilder();
        if (conditions != null)
        {
            for (Object condition : conditions)
            {
                sb.append(condition == null ? "-" : String.valueOf(condition)).append('~');
            }
        }
        return hash(sb.toString());
    }

    /**
     * 数据权限维度。凡结果随当前用户可见部门变化的缓存，key 必须带上它，否则会跨用户串数据。
     */
    public static String scopedDept()
    {
        return of(QtAuthUtils.scopedDepartment());
    }

    private static String fieldsOf(Object query)
    {
        if (query == null)
        {
            return "-";
        }
        List<String> parts = new ArrayList<>();
        for (Class<?> clazz = query.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass())
        {
            for (Field field : clazz.getDeclaredFields())
            {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic())
                {
                    continue;
                }
                Object value;
                try
                {
                    field.setAccessible(true);
                    value = field.get(query);
                }
                catch (Exception e)
                {
                    continue;
                }
                if (value == null || (value instanceof String && ((String) value).isEmpty()))
                {
                    continue;
                }
                if (value instanceof Map)
                {
                    Map<?, ?> map = (Map<?, ?>) value;
                    if (map.isEmpty())
                    {
                        continue;
                    }
                    List<String> entries = new ArrayList<>();
                    for (Map.Entry<?, ?> entry : map.entrySet())
                    {
                        entries.add(entry.getKey() + "=" + entry.getValue());
                    }
                    Collections.sort(entries);
                    parts.add(field.getName() + "{" + StringUtils.join(entries, ",") + "}");
                }
                else
                {
                    parts.add(field.getName() + "=" + value);
                }
            }
        }
        Collections.sort(parts);
        return parts.isEmpty() ? "-" : StringUtils.join(parts, ";");
    }

    private static String hash(String raw)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++)
            {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            // SHA-256 一定存在；兜底用原文，保证不会因为指纹算法把请求打挂
            return raw;
        }
    }
}
