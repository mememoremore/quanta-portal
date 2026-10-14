package com.ruoyi.framework.metrics;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 在整段请求结束后把耗时 / 状态码记入内存聚合器。
 */
public class AccessMetricsFilter extends OncePerRequestFilter
{
    private final AccessMetricsCollector collector;
    private final boolean enabled;

    public AccessMetricsFilter(AccessMetricsCollector collector, boolean enabled)
    {
        this.collector = collector;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        return !enabled || isExcludedPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        collector.beginRequest();
        long startNs = System.nanoTime();
        try
        {
            filterChain.doFilter(request, response);
        }
        finally
        {
            long costMs = (System.nanoTime() - startNs) / 1_000_000L;
            String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (pattern == null || pattern.isEmpty())
            {
                pattern = request.getRequestURI();
            }
            collector.record(pattern, request.getMethod(), response.getStatus(), costMs);
            collector.endRequest();
        }
    }

    static boolean isExcludedPath(String uri)
    {
        if (uri == null || uri.isEmpty())
        {
            return true;
        }
        return uri.startsWith("/profile/")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/druid/")
                || uri.startsWith("/qt/metrics")
                || uri.startsWith("/actuator")
                || "/favicon.ico".equals(uri)
                || "/error".equals(uri);
    }
}
