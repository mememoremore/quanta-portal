package com.ruoyi.framework.oauth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.web.service.RefreshSession;
import com.ruoyi.framework.web.service.TokenService;

@RestController
public class QuantaOAuthController
{
    static final String BROWSER_COOKIE = "QUANTA_OAUTH_BROWSER";

    private final QuantaOAuthFlow flow;

    private final TokenService tokenService;

    public QuantaOAuthController(QuantaOAuthFlow flow, TokenService tokenService)
    {
        this.flow = flow;
        this.tokenService = tokenService;
    }

    @GetMapping("/.well-known/openid-configuration")
    public void discovery(HttpServletResponse response) throws IOException
    {
        write(response, flow.discovery());
    }

    @GetMapping("/oauth2/jwks")
    public void jwks(HttpServletResponse response) throws IOException
    {
        write(response, flow.jwks());
    }

    @GetMapping("/oauth2/authorize")
    public void authorize(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        write(response, flow.authorize(parameters(request), browser(request), IpUtils.getIpAddr(request)));
    }

    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void token(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        write(response, flow.token(parameters(request), request.getHeader(HttpHeaders.AUTHORIZATION),
                IpUtils.getIpAddr(request)));
    }

    @GetMapping("/oauth2/interaction/{interactionId}")
    public void read(@PathVariable String interactionId, HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        write(response, flow.read(interactionId, browser(request), IpUtils.getIpAddr(request)));
    }

    @PostMapping("/oauth2/interaction/{interactionId}/decision")
    public void decide(@PathVariable String interactionId, @RequestBody JsonNode body, HttpServletRequest request,
            HttpServletResponse response) throws IOException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        RefreshSession refresh = tokenService.peekRefresh(request);
        write(response, flow.decide(interactionId, browser(request), request.getHeader("X-CSRF-Token"),
                request.getHeader(HttpHeaders.ORIGIN), loginUser, refresh == null ? null : refresh.getUserId(), body,
                IpUtils.getIpAddr(request)));
    }

    @GetMapping("/oauth2/interaction/{interactionId}/continue")
    public void continueLogin(@PathVariable String interactionId, HttpServletRequest request,
            HttpServletResponse response) throws IOException
    {
        RefreshSession refresh = tokenService.peekRefresh(request);
        write(response, flow.continueLogin(interactionId, browser(request),
                refresh == null ? null : refresh.getUserId(), IpUtils.getIpAddr(request)));
    }

    private static Map<String, List<String>> parameters(HttpServletRequest request)
    {
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(Arrays.asList(entry.getValue()))));
    }

    private static String browser(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
        {
            return null;
        }
        for (Cookie cookie : cookies)
        {
            if (BROWSER_COOKIE.equals(cookie.getName()))
            {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static void write(HttpServletResponse response, QuantaOAuthResponse result) throws IOException
    {
        if (result.browserCookie != null)
        {
            ResponseCookie cookie = ResponseCookie.from(BROWSER_COOKIE, result.browserCookie)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(java.time.Duration.ofDays(1))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        response.setStatus(result.status);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (result.redirect != null)
        {
            response.setHeader(HttpHeaders.LOCATION, result.redirect);
        }
        if (result.body != null && !result.body.isEmpty())
        {
            response.setCharacterEncoding("UTF-8");
            response.setContentType(result.contentType);
            response.getWriter().write(result.body);
        }
    }
}
