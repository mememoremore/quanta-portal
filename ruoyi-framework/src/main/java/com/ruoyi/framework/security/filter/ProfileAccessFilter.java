package com.ruoyi.framework.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.framework.security.ProfileAccessSigner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sensitive upload dirs require a valid HMAC query string; login tokens are not enough.
 */
@Component
public class ProfileAccessFilter extends OncePerRequestFilter
{
    private final ProfileAccessSigner profileAccessSigner;

    public ProfileAccessFilter(ProfileAccessSigner profileAccessSigner)
    {
        this.profileAccessSigner = profileAccessSigner;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/profile/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        String path = request.getRequestURI();
        if (profileAccessSigner.isPublicResource(path) || !profileAccessSigner.isSensitiveResource(path))
        {
            filterChain.doFilter(request, response);
            return;
        }
        if (profileAccessSigner.isValidRequest(request))
        {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"file access denied\"}");
    }
}
