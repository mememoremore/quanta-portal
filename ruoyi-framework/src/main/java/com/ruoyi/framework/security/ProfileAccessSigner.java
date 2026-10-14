package com.ruoyi.framework.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Catalog images stay anonymous; sensitive files need a short-lived HMAC query string
 * because &lt;img&gt; / mini-program &lt;image&gt; cannot send Authorization.
 */
@Component
public class ProfileAccessSigner
{
    public static final String PARAM_EXP = "exp";
    public static final String PARAM_SIG = "sig";

    private static final String[] PUBLIC_PREFIXES = {
        "/profile/upload/qt/clothing-item/",
        "/profile/upload/qt/payment-qr/"
    };

    private static final String[] SENSITIVE_PREFIXES = {
        "/profile/upload/qt/interview-photo/",
        "/profile/upload/qt/interview-resume/",
        "/profile/upload/qt/payment-proof/",
        "/profile/upload/qt/materials/"
    };

    private static final long DEFAULT_TTL_MS = TimeUnit.HOURS.toMillis(12);

    private final TokenService tokenService;

    public ProfileAccessSigner(TokenService tokenService)
    {
        this.tokenService = tokenService;
    }

    public boolean isPublicResource(String path)
    {
        return startsWithAny(normalizePath(path), PUBLIC_PREFIXES);
    }

    public boolean isSensitiveResource(String path)
    {
        return startsWithAny(normalizePath(path), SENSITIVE_PREFIXES);
    }

    public boolean isSensitiveDownloadResource(String resource)
    {
        if (StringUtils.isEmpty(resource))
        {
            return false;
        }
        String path = extractPath(resource.replace("\\", "/"));
        if (isSensitiveResource(path))
        {
            return true;
        }
        if (!path.startsWith("/profile/"))
        {
            return isSensitiveResource("/profile" + (path.startsWith("/") ? path : "/" + path));
        }
        return false;
    }

    public String signUrl(String url)
    {
        if (StringUtils.isEmpty(url))
        {
            return url;
        }
        String path = extractPath(url);
        if (!isSensitiveResource(path))
        {
            return url;
        }
        if (url.contains(PARAM_SIG + "="))
        {
            return url;
        }
        long exp = System.currentTimeMillis() + DEFAULT_TTL_MS;
        String sig = signature(path, exp);
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + PARAM_EXP + "=" + exp + "&" + PARAM_SIG + "=" + sig;
    }

    public boolean isValidRequest(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        String expStr = request.getParameter(PARAM_EXP);
        String sig = request.getParameter(PARAM_SIG);
        if (StringUtils.isEmpty(expStr) || StringUtils.isEmpty(sig))
        {
            return false;
        }
        long exp;
        try
        {
            exp = Long.parseLong(expStr);
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        if (exp < System.currentTimeMillis())
        {
            return false;
        }
        return MessageDigest.isEqual(signature(path, exp).getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8));
    }

    private String signature(String path, long exp)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenService.getSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((normalizePath(path) + "|" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("cannot sign profile url", e);
        }
    }

    private static boolean startsWithAny(String path, String[] prefixes)
    {
        for (String prefix : prefixes)
        {
            if (path.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    private static String extractPath(String url)
    {
        String path = url;
        int scheme = path.indexOf("://");
        if (scheme >= 0)
        {
            int slash = path.indexOf('/', scheme + 3);
            path = slash >= 0 ? path.substring(slash) : "/";
        }
        int q = path.indexOf('?');
        if (q >= 0)
        {
            path = path.substring(0, q);
        }
        return normalizePath(path);
    }

    private static String normalizePath(String path)
    {
        if (path == null)
        {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
