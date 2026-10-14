package com.ruoyi.common.filter;

import java.io.IOException;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * Bind X-Trace-Id to SLF4J MDC and echo it on the response.
 */
public class TraceIdFilter extends OncePerRequestFilter
{
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String MDC_KEY = "traceId";

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        try
        {
            MDC.put(MDC_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        }
        finally
        {
            MDC.remove(MDC_KEY);
        }
    }

    static String resolveTraceId(String headerValue)
    {
        if (StringUtils.isNotEmpty(headerValue) && TRACE_ID_PATTERN.matcher(headerValue).matches())
        {
            return headerValue;
        }
        return IdUtils.fastSimpleUUID();
    }
}
