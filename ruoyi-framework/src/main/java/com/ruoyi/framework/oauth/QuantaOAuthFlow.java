package com.ruoyi.framework.oauth;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;

@Service
public class QuantaOAuthFlow
{
    static final String ID_PATTERN = "^[A-Za-z0-9_-]{32,128}$";

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Set<String> AUTHORIZE_PARAMS = Set.of("response_type", "client_id", "redirect_uri", "scope",
            "state", "nonce", "code_challenge", "code_challenge_method");

    private final QuantaOAuthProperties properties;

    private final QuantaOAuthTokenIssuer issuer;

    private final QuantaOAuthInteractionStore interactions;

    private final QuantaOAuthCodeStore codes;

    private final QuantaOAuthRateLimiter limiter;

    private final RegisteredClientRepository clients;

    private final ISysUserService users;

    private final ObjectMapper mapper = new ObjectMapper();

    public QuantaOAuthFlow(QuantaOAuthProperties properties, QuantaOAuthTokenIssuer issuer,
            QuantaOAuthInteractionStore interactions, QuantaOAuthCodeStore codes, QuantaOAuthRateLimiter limiter,
            RegisteredClientRepository clients, ISysUserService users)
    {
        this.properties = properties;
        this.issuer = issuer;
        this.interactions = interactions;
        this.codes = codes;
        this.limiter = limiter;
        this.clients = clients;
        this.users = users;
    }

    public QuantaOAuthResponse discovery()
    {
        if (!issuer.ready())
        {
            return oauthError(503, "temporarily_unavailable");
        }
        return QuantaOAuthResponse.json(200, write(QuantaOAuthTokenIssuer.discovery(properties.getIssuer())));
    }

    public QuantaOAuthResponse jwks()
    {
        if (!issuer.ready())
        {
            return oauthError(503, "temporarily_unavailable");
        }
        return QuantaOAuthResponse.json(200, write(issuer.publicJwkSet()));
    }

    public QuantaOAuthResponse authorize(Map<String, List<String>> params, String browserId, String ip)
    {
        if (!issuer.ready())
        {
            return QuantaOAuthResponse.html(503, "授权服务尚未就绪，请稍后重试或联系管理员。");
        }
        if (!limiter.allow("authorize", ip))
        {
            return QuantaOAuthResponse.html(429, "请求过于频繁，请稍后重试。");
        }
        if (!authorizeParamsValid(params))
        {
            return QuantaOAuthResponse.html(400, "授权请求无效，请从 Challenge 重新发起登录。");
        }
        String browser = validId(browserId) ? browserId : randomId();
        String cookie = browser.equals(browserId) ? null : browser;
        QuantaOAuthInteraction interaction = new QuantaOAuthInteraction();
        interaction.id = randomId();
        interaction.clientId = properties.getClientId();
        interaction.redirectUri = properties.getRedirectUri();
        interaction.state = only(params, "state");
        interaction.nonce = only(params, "nonce");
        interaction.codeChallenge = only(params, "code_challenge");
        interaction.browserId = browser;
        interaction.csrfToken = randomId();
        interaction.status = "pending";
        interaction.expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L;
        interactions.create(interaction);
        String location = properties.getPortalOrigin() + "/oauth/authorize?interaction=" + interaction.id;
        return QuantaOAuthResponse.redirect(302, location, cookie);
    }

    public QuantaOAuthResponse read(String id, String browserId, String ip)
    {
        if (!issuer.ready())
        {
            return envelope(503, "授权服务尚未就绪，请稍后重试或联系管理员。", null);
        }
        if (!limiter.allow("interaction", ip))
        {
            return envelope(429, "请求过于频繁，请稍后重试。", null);
        }
        QuantaOAuthInteraction interaction = requireBrowser(id, browserId);
        if (interaction == null)
        {
            return envelope(interactionStatus(id), interactionStatus(id) == 410 ? "本次授权已过期，请从 Challenge 重新登录。"
                    : "授权请求与当前浏览器不匹配", null);
        }
        ObjectNode data = mapper.createObjectNode();
        data.put("interactionId", interaction.id);
        data.put("status", interaction.status);
        data.put("expiresAt", Instant.ofEpochMilli(interaction.expiresAt).toString());
        data.put("csrfToken", interaction.csrfToken);
        data.put("consentRequired", true);
        data.putArray("scopes").add("openid").add("profile");
        if ("approved".equals(interaction.status) && StringUtils.isNotEmpty(interaction.subjectUserId))
        {
            data.put("subjectUserId", interaction.subjectUserId);
        }
        ObjectNode client = data.putObject("client");
        client.put("clientId", properties.getClientId());
        client.put("name", properties.getClientName());
        client.put("origin", properties.clientOrigin());
        client.put("redirectUri", properties.getRedirectUri());
        return envelope(200, "操作成功", data);
    }

    public QuantaOAuthResponse decide(String id, String browserId, String csrf, String origin, LoginUser loginUser,
            Long refreshUserId, JsonNode body, String ip)
    {
        if (!issuer.ready())
        {
            return envelope(503, "授权服务尚未就绪，请稍后重试或联系管理员。", null);
        }
        if (!limiter.allow("decision", ip))
        {
            return envelope(429, "请求过于频繁，请稍后重试。", null);
        }
        QuantaOAuthInteraction interaction = requireBrowser(id, browserId);
        if (interaction == null)
        {
            int status = interactionStatus(id);
            return envelope(status, status == 410 ? "本次授权已过期，请从 Challenge 重新登录。" : "授权请求与当前浏览器不匹配", null);
        }
        if (!properties.getPortalOrigin().equals(origin) || !interaction.csrfToken.equals(csrf))
        {
            return envelope(403, "授权请求校验失败", null);
        }
        String action = action(body);
        if (action == null)
        {
            return envelope(400, "授权决定无效", null);
        }
        long now = System.currentTimeMillis();
        if ("deny".equals(action))
        {
            String result = interactions.deny(id, now);
            if (!"ok".equals(result))
            {
                return terminal(result);
            }
            String redirect = properties.getRedirectUri() + "?error=access_denied&state="
                    + URLEncoder.encode(interaction.state, StandardCharsets.UTF_8);
            ObjectNode data = mapper.createObjectNode();
            data.put("redirectUrl", redirect);
            return envelope(200, "操作成功", data);
        }
        if (loginUser == null || loginUser.getUserId() == null)
        {
            return envelope(401, "请先登录塔员账号", null);
        }
        if (refreshUserId == null || !refreshUserId.equals(loginUser.getUserId()))
        {
            return envelope(403, "主站登录状态已变化，请重新登录后再授权", null);
        }
        SysUser user = users.selectUserById(loginUser.getUserId());
        if (!member(user))
        {
            return envelope(403, "当前账号不能授权登录 Challenge", null);
        }
        String result = interactions.approve(id, String.valueOf(user.getUserId()), now);
        if (!"ok".equals(result) && !"same".equals(result))
        {
            return terminal(result);
        }
        ObjectNode data = mapper.createObjectNode();
        data.put("status", "approved");
        data.put("continuePath", "/oauth2/interaction/" + id + "/continue");
        data.put("expiresAt", Instant.ofEpochMilli(interaction.expiresAt).toString());
        return envelope(200, "操作成功", data);
    }

    public QuantaOAuthResponse continueLogin(String id, String browserId, Long refreshUserId, String ip)
    {
        if (!issuer.ready())
        {
            return QuantaOAuthResponse.html(503, "授权服务尚未就绪，请稍后重试或联系管理员。");
        }
        if (!limiter.allow("continue", ip))
        {
            return QuantaOAuthResponse.html(429, "请求过于频繁，请稍后重试。");
        }
        QuantaOAuthInteraction interaction = requireBrowser(id, browserId);
        if (interaction == null)
        {
            return QuantaOAuthResponse.html(interactionStatus(id) == 410 ? 410 : 403, "本次授权已失效，请从 Challenge 重新发起登录。");
        }
        if (!"approved".equals(interaction.status) || StringUtils.isEmpty(interaction.subjectUserId))
        {
            return QuantaOAuthResponse.html(403, "授权尚未完成，请返回确认页面。");
        }
        if (refreshUserId == null || !interaction.subjectUserId.equals(String.valueOf(refreshUserId)))
        {
            return QuantaOAuthResponse.html(403, "授权账号已变化，请重新从 Challenge 发起登录。");
        }
        SysUser user = users.selectUserById(refreshUserId);
        if (!member(user))
        {
            return QuantaOAuthResponse.html(403, "当前账号不能授权登录 Challenge。");
        }
        String result = interactions.consume(id, System.currentTimeMillis());
        if (!"ok".equals(result))
        {
            return QuantaOAuthResponse.html(409, "本次授权已结束，请从 Challenge 重新发起登录。");
        }
        String code = randomId();
        QuantaOAuthCode stored = new QuantaOAuthCode();
        stored.clientId = interaction.clientId;
        stored.redirectUri = interaction.redirectUri;
        stored.userId = String.valueOf(user.getUserId());
        stored.nonce = interaction.nonce;
        stored.codeChallenge = interaction.codeChallenge;
        stored.expiresAt = System.currentTimeMillis() + 60_000L;
        codes.save(code, stored);
        String location = interaction.redirectUri + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(interaction.state, StandardCharsets.UTF_8);
        return QuantaOAuthResponse.redirect(303, location, null);
    }

    public QuantaOAuthResponse token(Map<String, List<String>> params, String authorization, String ip)
    {
        if (!issuer.ready())
        {
            return oauthError(503, "temporarily_unavailable");
        }
        if (!limiter.allow("token", ip))
        {
            return oauthError(429, "temporarily_unavailable");
        }
        String[] clientAuth = basic(authorization);
        RegisteredClient client = clientAuth == null ? null : clients.findByClientId(clientAuth[0]);
        if (client == null || client.getClientSecret() == null
                || !new BCryptPasswordEncoder().matches(clientAuth[1], client.getClientSecret()))
        {
            return oauthError(401, "invalid_client");
        }
        if (params.keySet().stream().anyMatch(key -> !Set.of("grant_type", "code", "redirect_uri", "code_verifier").contains(key))
                || !"authorization_code".equals(only(params, "grant_type")) || only(params, "code") == null
                || only(params, "redirect_uri") == null || only(params, "code_verifier") == null)
        {
            return oauthError(400, "invalid_request");
        }
        QuantaOAuthCode code = codes.consume(only(params, "code"));
        if (code == null || !client.getClientId().equals(code.clientId) || !code.redirectUri.equals(only(params, "redirect_uri"))
                || !properties.getRedirectUri().equals(code.redirectUri) || !pkce(only(params, "code_verifier"), code.codeChallenge))
        {
            return oauthError(400, "invalid_grant");
        }
        SysUser user = users.selectUserById(Long.valueOf(code.userId));
        if (!member(user))
        {
            return oauthError(400, "invalid_grant");
        }
        Instant now = Instant.now();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", issuer.accessToken(String.valueOf(user.getUserId()), now));
        body.put("token_type", "Bearer");
        body.put("expires_in", QuantaOAuthTokenIssuer.ACCESS_TOKEN_TTL.toSeconds());
        body.put("id_token", issuer.idToken(String.valueOf(user.getUserId()), displayName(user), code.nonce, now));
        body.put("scope", "openid profile");
        return QuantaOAuthResponse.json(200, write(body));
    }

    static boolean member(SysUser user)
    {
        if (user == null || user.getUserId() == null)
        {
            return false;
        }
        if (user.getDelFlag() != null && !"0".equals(user.getDelFlag()))
        {
            return false;
        }
        if (user.getStatus() != null && !"0".equals(user.getStatus()))
        {
            return false;
        }
        return "1".equals(user.getIsQuantaMember());
    }

    static String displayName(SysUser user)
    {
        return StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    static boolean pkce(String verifier, String challenge)
    {
        if (verifier == null || challenge == null || !verifier.matches("[A-Za-z0-9._~-]{43,128}")
                || !challenge.matches("[A-Za-z0-9_-]{43,128}"))
        {
            return false;
        }
        try
        {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return MessageDigest.isEqual(computed.getBytes(StandardCharsets.US_ASCII),
                    challenge.getBytes(StandardCharsets.US_ASCII));
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private QuantaOAuthInteraction requireBrowser(String id, String browserId)
    {
        if (!validId(id) || !validId(browserId))
        {
            return null;
        }
        QuantaOAuthInteraction interaction = interactions.find(id);
        if (interaction == null || interaction.expiresAt <= System.currentTimeMillis() || interaction.consumed
                || !"pending".equals(interaction.status) && !"approved".equals(interaction.status))
        {
            return null;
        }
        if (!browserId.equals(interaction.browserId))
        {
            return null;
        }
        return interaction;
    }

    private int interactionStatus(String id)
    {
        QuantaOAuthInteraction interaction = validId(id) ? interactions.find(id) : null;
        if (interaction == null || interaction.expiresAt <= System.currentTimeMillis())
        {
            return 410;
        }
        return 403;
    }

    private QuantaOAuthResponse terminal(String result)
    {
        if ("expired".equals(result) || "missing".equals(result))
        {
            return envelope(410, "本次授权已过期，请从 Challenge 重新登录。", null);
        }
        if ("conflict".equals(result))
        {
            return envelope(409, "授权账号与已确认的账号不一致", null);
        }
        return envelope(409, "授权请求已结束", null);
    }

    private boolean authorizeParamsValid(Map<String, List<String>> params)
    {
        if (params == null || !AUTHORIZE_PARAMS.equals(params.keySet()))
        {
            return false;
        }
        if (!"code".equals(only(params, "response_type")) || !properties.getClientId().equals(only(params, "client_id"))
                || !properties.getRedirectUri().equals(only(params, "redirect_uri"))
                || !"S256".equals(only(params, "code_challenge_method")))
        {
            return false;
        }
        String scope = only(params, "scope");
        if (scope == null || !Set.of(scope.split(" ")).equals(Set.of("openid", "profile")) || scope.split(" ").length != 2)
        {
            return false;
        }
        return bounded(only(params, "state")) && bounded(only(params, "nonce"))
                && only(params, "code_challenge") != null && only(params, "code_challenge").matches("[A-Za-z0-9_-]{43,128}");
    }

    private static boolean bounded(String value)
    {
        return value != null && value.length() >= 8 && value.length() <= 512 && value.indexOf('\r') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\0') < 0;
    }

    private static String only(Map<String, List<String>> params, String name)
    {
        List<String> values = params.get(name);
        if (values == null || values.size() != 1 || StringUtils.isEmpty(values.get(0)))
        {
            return null;
        }
        return values.get(0);
    }

    private static String action(JsonNode body)
    {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("action"))
        {
            return null;
        }
        String action = body.get("action").asText();
        return "approve".equals(action) || "deny".equals(action) ? action : null;
    }

    private static boolean validId(String value)
    {
        return value != null && value.matches(ID_PATTERN);
    }

    private static String randomId()
    {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String[] basic(String header)
    {
        if (header == null || !header.startsWith("Basic "))
        {
            return null;
        }
        try
        {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int split = decoded.indexOf(':');
            if (split <= 0)
            {
                return null;
            }
            return new String[] { URLDecoder.decode(decoded.substring(0, split), StandardCharsets.UTF_8),
                    URLDecoder.decode(decoded.substring(split + 1), StandardCharsets.UTF_8) };
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    private QuantaOAuthResponse envelope(int status, String message, ObjectNode data)
    {
        ObjectNode root = mapper.createObjectNode();
        root.put("code", status);
        root.put("msg", message);
        if (data != null)
        {
            root.set("data", data);
        }
        return QuantaOAuthResponse.json(status, write(root));
    }

    private QuantaOAuthResponse oauthError(int status, String error)
    {
        ObjectNode root = mapper.createObjectNode();
        root.put("error", error);
        return QuantaOAuthResponse.json(status, write(root));
    }

    private String write(Object value)
    {
        try
        {
            return mapper.writeValueAsString(value);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("授权响应生成失败");
        }
    }
}

@Component
class QuantaRegisteredClientRepository implements RegisteredClientRepository
{
    private final RegisteredClient client;

    QuantaRegisteredClientRepository(QuantaOAuthProperties properties, BCryptPasswordEncoder encoder)
    {
        if (!properties.ready())
        {
            this.client = null;
            return;
        }
        this.client = RegisteredClient.withId("quanta-challenge")
                .clientId(properties.getClientId())
                .clientSecret(encoder.encode(properties.getClientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(properties.getRedirectUri())
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder().idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                        .authorizationCodeTimeToLive(java.time.Duration.ofSeconds(60))
                        .accessTokenTimeToLive(QuantaOAuthTokenIssuer.ACCESS_TOKEN_TTL)
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }

    @Override
    public void save(RegisteredClient registeredClient)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public RegisteredClient findById(String id)
    {
        return client != null && client.getId().equals(id) ? client : null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId)
    {
        return client != null && client.getClientId().equals(clientId) ? client : null;
    }
}
