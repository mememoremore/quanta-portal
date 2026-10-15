package com.ruoyi.framework.web.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.UserAgentUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * token验证处理
 * 
 * @author ruoyi
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // Access 有效期（分钟）
    @Value("${token.expireTime}")
    private int expireTime;

    @Value("${token.refresh.cookie-name:quanta_refresh}")
    private String refreshCookieName;

    @Value("${token.refresh.secure:false}")
    private boolean refreshSecure;

    @Value("${token.refresh.member-expire-days:7}")
    private int refreshMemberExpireDays;

    @Value("${token.refresh.admin-expire-days:1}")
    private int refreshAdminExpireDays;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final int SESSION_INDEX_TTL_MINUTES = 8 * 24 * 60;

    private static final Set<String> PRIVILEGED_ROLE_KEYS = Set.of("admin", "ceo", "qt_mgmt", "qt_manager");

    private final SecureRandom secureRandom = new SecureRandom();

    /** 若依上游公开在仓库里的默认密钥，禁止在任何环境使用 */
    private static final String DEFAULT_PUBLISHED_SECRET = "abcdefghijklmnopqrstuvwxyz";

    @Autowired
    private RedisCache redisCache;

    /**
     * 令牌密钥兜底。
     * <p>
     * 上游把默认密钥写死在 application.yml 里，而本仓库是公开 fork —— 任何人都能据此伪造管理员 token。
     * 因此这里不再接受默认值：未配置时随机生成一份（重启即失效，仅够本地开发用），并打醒目告警。
     * 生产由 deploy/aliyun/run.sh 从 .env 强制注入 TOKEN_SECRET。
     */
    @PostConstruct
    public void initSecret()
    {
        if (StringUtils.isEmpty(secret) || DEFAULT_PUBLISHED_SECRET.equals(secret) || secret.length() < 32)
        {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.secret = Base64.getEncoder().encodeToString(random);
            log.warn("未配置有效的 token.secret（TOKEN_SECRET），已启用一次性随机密钥："
                    + "服务重启后所有已签发令牌立即失效，且多实例之间无法互认。生产环境请在 deploy/aliyun/.env 中配置 TOKEN_SECRET。");
        }
    }

    /**
     * 当前实际使用的签名密钥（含本地随机兜底后的值），供文件访问签名等复用。
     */
    public String getSigningSecret()
    {
        return secret;
    }

    /**
     * 获取用户身份信息
     * 
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                LoginUser user = redisCache.getCacheObject(userKey);
                return user;
            }
            catch (Exception e)
            {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 更新缓存中的用户身份，保留剩余 Access 有效期。
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            writeLoginUser(loginUser, false);
        }
    }

    /**
     * 删除用户身份信息
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isEmpty(token))
        {
            return;
        }
        LoginUser cached = redisCache.getCacheObject(getTokenKey(token));
        redisCache.deleteObject(getTokenKey(token));
        if (cached != null)
        {
            forget(userAccessKey(cached.getUserId()), token);
        }
    }

    public int getAccessExpiresIn()
    {
        return expireTime * 60;
    }

    /**
     * 为当前响应签发 Refresh Cookie。没有 HTTP 响应时（例如非 Web 调用）直接跳过。
     */
    public void issueRefreshForCurrentResponse(LoginUser loginUser)
    {
        ServletRequestAttributes attributes = currentRequestAttributes();
        if (attributes == null || attributes.getResponse() == null)
        {
            return;
        }
        issueRefresh(attributes.getResponse(), loginUser, null);
    }

    public void clearRefreshCookieForCurrentResponse()
    {
        ServletRequestAttributes attributes = currentRequestAttributes();
        if (attributes == null || attributes.getResponse() == null)
        {
            return;
        }
        clearRefreshCookie(attributes.getResponse());
    }

    /**
     * 签发新的 Refresh，并写入 HttpOnly Cookie。familyId 为空时开启新的轮换家族。
     */
    public void issueRefresh(HttpServletResponse response, LoginUser loginUser, String familyId)
    {
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = sha256(token);
        RefreshSession session = new RefreshSession();
        session.setUserId(loginUser.getUserId());
        session.setUsername(loginUser.getUsername());
        session.setAccessUuid(loginUser.getToken());
        session.setFamilyId(StringUtils.isNotEmpty(familyId) ? familyId : IdUtils.fastUUID());
        int days = refreshDays(loginUser);
        redisCache.setCacheObject(refreshKey(hash), session, days, TimeUnit.DAYS);
        remember(userRefreshKey(loginUser.getUserId()), hash);
        writeRefreshCookie(response, token, days * 24L * 60L * 60L);
    }

    /**
     * 校验并消费当前 Cookie 中的 Refresh。成功后旧值立即失效。
     * 再次出示已轮换的值会撤销该用户全部会话。
     */
    public RefreshSession consumeRefresh(HttpServletRequest request, HttpServletResponse response)
    {
        String raw = readRefreshCookie(request);
        if (StringUtils.isEmpty(raw))
        {
            return null;
        }
        String hash = sha256(raw);
        RefreshSession session = redisCache.getCacheObject(refreshKey(hash));
        if (session == null)
        {
            Long replayedUserId = redisCache.getCacheObject(refreshUsedKey(hash));
            if (replayedUserId != null)
            {
                revokeUserSessions(replayedUserId);
            }
            clearRefreshCookie(response);
            return null;
        }
        redisCache.deleteObject(refreshKey(hash));
        forget(userRefreshKey(session.getUserId()), hash);
        redisCache.setCacheObject(refreshUsedKey(hash), session.getUserId(), refreshMemberExpireDays, TimeUnit.DAYS);
        return session;
    }

    /**
     * 退出时只撤销这次出示的 Refresh，不影响同一用户的其他设备。
     */
    public void revokePresentedRefresh(HttpServletRequest request)
    {
        String raw = readRefreshCookie(request);
        if (StringUtils.isEmpty(raw))
        {
            return;
        }
        String hash = sha256(raw);
        RefreshSession session = redisCache.getCacheObject(refreshKey(hash));
        if (session == null)
        {
            return;
        }
        redisCache.deleteObject(refreshKey(hash));
        forget(userRefreshKey(session.getUserId()), hash);
        redisCache.setCacheObject(refreshUsedKey(hash), session.getUserId(), refreshMemberExpireDays, TimeUnit.DAYS);
    }

    public void clearRefreshCookie(HttpServletResponse response)
    {
        if (response == null)
        {
            return;
        }
        writeRefreshCookie(response, "", 0);
    }

    /**
     * 改密、停用、接受录用时撤销该用户全部 Access 与 Refresh。
     */
    public void revokeUserSessions(Long userId)
    {
        if (userId == null)
        {
            return;
        }
        Set<String> accessIds = redisCache.getCacheObject(userAccessKey(userId));
        if (accessIds != null)
        {
            for (String accessId : new HashSet<>(accessIds))
            {
                redisCache.deleteObject(getTokenKey(accessId));
            }
        }
        redisCache.deleteObject(userAccessKey(userId));
        Set<String> refreshHashes = redisCache.getCacheObject(userRefreshKey(userId));
        if (refreshHashes != null)
        {
            for (String hash : new HashSet<>(refreshHashes))
            {
                redisCache.deleteObject(refreshKey(hash));
            }
        }
        redisCache.deleteObject(userRefreshKey(userId));
    }

    /**
     * 创建令牌
     * 
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        return createToken(claims);
    }

    /**
     * Access 到期后由 /auth/refresh 换新，请求过程中不再滑动续期。
     */
    public void verifyToken(LoginUser loginUser)
    {
        if (loginUser == null)
        {
            return;
        }
    }

    /**
     * 签发 Access 时写入 Redis，TTL 固定为 token.expireTime。
     */
    public void refreshToken(LoginUser loginUser)
    {
        writeLoginUser(loginUser, true);
    }

    private void writeLoginUser(LoginUser loginUser, boolean renew)
    {
        String userKey = getTokenKey(loginUser.getToken());
        int ttlSeconds;
        if (renew)
        {
            loginUser.setLoginTime(System.currentTimeMillis());
            loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
            ttlSeconds = expireTime * 60;
        }
        else
        {
            long remainMs = loginUser.getExpireTime() == null
                    ? expireTime * MILLIS_MINUTE
                    : loginUser.getExpireTime() - System.currentTimeMillis();
            if (remainMs <= 0)
            {
                delLoginUser(loginUser.getToken());
                return;
            }
            ttlSeconds = (int) Math.max(1, remainMs / MILLIS_SECOND);
        }
        redisCache.setCacheObject(userKey, loginUser, ttlSeconds, TimeUnit.SECONDS);
        remember(userAccessKey(loginUser.getUserId()), loginUser.getToken());
    }

    /**
     * 设置用户代理信息
     * 
     * @param loginUser 登录信息
     */
    public void setUserAgent(LoginUser loginUser)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getBrowser(userAgent));
        loginUser.setOs(UserAgentUtils.getOperatingSystem(userAgent));
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims)
    {
        return Jwts.builder()
                .claims(claims)
                .signWith(jwtKey())
                .compact();
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .verifyWith(jwtKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey jwtKey()
    {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 64)
        {
            try
            {
                bytes = MessageDigest.getInstance("SHA-512").digest(bytes);
            }
            catch (Exception e)
            {
                throw new IllegalStateException("无法派生 JWT 密钥", e);
            }
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    private String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    private int refreshDays(LoginUser loginUser)
    {
        if (loginUser != null && loginUser.getPermissions() != null
                && loginUser.getPermissions().contains("*:*:*"))
        {
            return refreshAdminExpireDays;
        }
        if (loginUser != null && loginUser.getUser() != null && loginUser.getUser().getRoles() != null)
        {
            for (SysRole role : loginUser.getUser().getRoles())
            {
                if (role != null && PRIVILEGED_ROLE_KEYS.contains(role.getRoleKey()))
                {
                    return refreshAdminExpireDays;
                }
            }
        }
        return refreshMemberExpireDays;
    }

    private void remember(String key, String value)
    {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value))
        {
            return;
        }
        Set<String> current = redisCache.getCacheObject(key);
        Set<String> next = current == null ? new HashSet<>() : new HashSet<>(current);
        next.add(value);
        redisCache.setCacheObject(key, next, SESSION_INDEX_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private void forget(String key, String value)
    {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value))
        {
            return;
        }
        Set<String> current = redisCache.getCacheObject(key);
        if (current == null)
        {
            return;
        }
        Set<String> next = new HashSet<>(current);
        next.remove(value);
        if (next.isEmpty())
        {
            redisCache.deleteObject(key);
            return;
        }
        redisCache.setCacheObject(key, next, SESSION_INDEX_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private void writeRefreshCookie(HttpServletResponse response, String value, long maxAgeSeconds)
    {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, value)
                .httpOnly(true)
                .secure(refreshSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readRefreshCookie(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
        {
            return null;
        }
        for (Cookie cookie : cookies)
        {
            if (refreshCookieName.equals(cookie.getName()) && StringUtils.isNotEmpty(cookie.getValue()))
            {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static ServletRequestAttributes currentRequestAttributes()
    {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes))
        {
            return null;
        }
        return attributes;
    }

    private static String sha256(String value)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest)
            {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("无法计算 Refresh 哈希", e);
        }
    }

    private static String refreshKey(String hash)
    {
        return CacheConstants.REFRESH_TOKEN_KEY + hash;
    }

    private static String refreshUsedKey(String hash)
    {
        return CacheConstants.REFRESH_USED_KEY + hash;
    }

    private static String userAccessKey(Long userId)
    {
        return userId == null ? null : CacheConstants.LOGIN_USER_TOKENS_KEY + userId;
    }

    private static String userRefreshKey(Long userId)
    {
        return userId == null ? null : CacheConstants.REFRESH_USER_KEY + userId;
    }

    /**
     * 角色权限变更后，刷新所有持有该角色的在线用户权限
     *
     * @param roleId            变更的角色ID
     * @param permissionService 权限服务
     */
    public void refreshPermissionByRoleId(Long roleId, SysPermissionService permissionService)
    {
        // 扫描所有在线 token
        String pattern = CacheConstants.LOGIN_TOKEN_KEY + "*";
        Collection<String> keys = redisCache.keys(pattern);
        if (keys == null || keys.isEmpty())
        {
            return;
        }
        for (String key : keys)
        {
            LoginUser loginUser = redisCache.getCacheObject(key);
            if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().isAdmin())
            {
                // 管理员拥有所有权限，跳过
                continue;
            }
            // 判断该用户是否拥有此角色
            boolean hasRole = loginUser.getUser().getRoles() != null
                    && loginUser.getUser().getRoles().stream().anyMatch(r -> roleId.equals(r.getRoleId()));
            if (!hasRole)
            {
                continue;
            }
            // 刷新权限缓存
            loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
            setLoginUser(loginUser);
            log.info("角色[{}]权限变更，已刷新在线用户[{}]的权限缓存", roleId, loginUser.getUsername());
        }
    }
}
