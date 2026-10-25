package com.ruoyi.framework.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 门户可信来源策略。
 *
 * Pages 成员站点（{slug}.page.quantacenter.com）与门户同主域，浏览器会自动携带
 * 门户的 Refresh Cookie，因此 /auth/refresh 必须在消费 Cookie 前把请求来源限制为
 * 明确配置的门户 origin；Pages 子域永远不受信。启用 Pages 时 CORS 来源必须收紧为
 * 精确列表，禁止通配符。
 */
@Component
public class TrustedPortalOriginPolicy
{
    @Value("${ruoyi.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${qt.pages.enabled:false}")
    private boolean pagesEnabled;

    @Value("${qt.pages.base-domain:page.quantacenter.com}")
    private String pagesBaseDomain;

    /**
     * 校验 /auth/refresh 的请求来源；不通过时抛 403 业务异常，调用方负责转换真实 HTTP 状态。
     */
    public void assertRefreshAllowed(HttpServletRequest request)
    {
        if (!isRefreshAllowed(request))
        {
            throw new ServiceException("请求来源不受信任，请从门户访问", HttpStatus.FORBIDDEN);
        }
    }

    public boolean isRefreshAllowed(HttpServletRequest request)
    {
        String origin = request.getHeader("Origin");
        if (origin != null)
        {
            // 沙箱 iframe/file:// 会发送字面量 "null"，一律拒绝
            if ("null".equals(origin))
            {
                return false;
            }
            return isTrustedOrigin(origin);
        }
        String referer = request.getHeader("Referer");
        if (StringUtils.isNotEmpty(referer))
        {
            String refererOrigin = originOf(referer);
            return refererOrigin != null && isTrustedOrigin(refererOrigin);
        }
        return false;
    }

    /**
     * Pages 启用前置条件：CORS 来源必须至少包含一个精确 origin，禁止通配符或空白配置。
     */
    public void assertStrictOriginsForPages()
    {
        if (!pagesEnabled)
        {
            return;
        }
        if (exactOrigins().isEmpty())
        {
            throw new IllegalStateException(
                    "qt.pages.enabled 开启时 ruoyi.cors.allowed-origins 必须配置精确门户来源，禁止 * 或通配符");
        }
    }

    /**
     * 配置中的精确 origin 列表（过滤空白与通配符项）。
     */
    public List<String> exactOrigins()
    {
        List<String> exact = new ArrayList<>();
        if (StringUtils.isEmpty(allowedOrigins))
        {
            return exact;
        }
        for (String raw : allowedOrigins.split(","))
        {
            String value = raw.trim();
            if (value.isEmpty() || value.contains("*"))
            {
                continue;
            }
            exact.add(value);
        }
        return exact;
    }

    public boolean isTrustedOrigin(String origin)
    {
        if (StringUtils.isEmpty(origin) || isPagesOrigin(origin))
        {
            return false;
        }
        String candidate = normalize(origin);
        for (String exact : exactOrigins())
        {
            if (candidate.equals(normalize(exact)))
            {
                return true;
            }
        }
        // Pages 关闭时保留旧的通配回退行为（本地开发默认 *，生产由 CORS_ALLOWED_ORIGINS 收紧）
        return !pagesEnabled && wildcardActive();
    }

    public boolean isPagesOrigin(String origin)
    {
        String host = hostOf(origin);
        if (host == null || StringUtils.isEmpty(pagesBaseDomain))
        {
            return false;
        }
        String base = pagesBaseDomain.trim().toLowerCase(Locale.ROOT);
        return host.equals(base) || host.endsWith("." + base);
    }

    private boolean wildcardActive()
    {
        if (StringUtils.isEmpty(allowedOrigins))
        {
            return true;
        }
        boolean hasEntry = false;
        for (String raw : allowedOrigins.split(","))
        {
            String value = raw.trim();
            if (value.isEmpty())
            {
                continue;
            }
            hasEntry = true;
            if (value.contains("*"))
            {
                return true;
            }
        }
        // 与 ResourcesConfig.corsFilter 的回退一致：无有效条目时按 * 处理
        return !hasEntry;
    }

    private String normalize(String origin)
    {
        String value = origin.trim();
        while (value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String hostOf(String url)
    {
        try
        {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : null;
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    private String originOf(String url)
    {
        try
        {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null)
            {
                return null;
            }
            StringBuilder origin = new StringBuilder(scheme.toLowerCase(Locale.ROOT)).append("://").append(host.toLowerCase(Locale.ROOT));
            int port = uri.getPort();
            boolean defaultPort = ("https".equalsIgnoreCase(scheme) && port == 443)
                    || ("http".equalsIgnoreCase(scheme) && port == 80);
            if (port != -1 && !defaultPort)
            {
                origin.append(':').append(port);
            }
            return origin.toString();
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }
}
