package com.ruoyi.framework.oauth;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuantaOAuthFlowTest
{
    private static String pem;

    private final ObjectMapper mapper = new ObjectMapper();

    private String lastTokenBody;

    private QuantaOAuthProperties properties;

    private QuantaOAuthTokenIssuer issuer;

    private QuantaOAuthInteractionStore.Memory interactions;

    private QuantaOAuthCodeStore.Memory codes;

    private ISysUserService users;

    private QuantaOAuthFlow flow;

    @BeforeAll
    static void key() throws Exception
    {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }

    @BeforeEach
    void setup()
    {
        properties = new QuantaOAuthProperties();
        properties.setEnabled(true);
        properties.setIssuer("https://www.quantacenter.com");
        properties.setPortalOrigin("https://www.quantacenter.com");
        properties.setClientId("quanta-challenge");
        properties.setClientSecret("test-client-secret-must-be-32-characters");
        properties.setRedirectUri("https://challenge.quantacenter.com/api/auth/quanta/callback");
        properties.setPrivateKeyPem(pem);
        properties.setKeyId("quanta-oidc-1");
        issuer = new QuantaOAuthTokenIssuer(properties);
        issuer.loadKey();
        interactions = new QuantaOAuthInteractionStore.Memory();
        codes = new QuantaOAuthCodeStore.Memory();
        users = mock(ISysUserService.class);
        flow = new QuantaOAuthFlow(properties, issuer, interactions, codes, new QuantaOAuthRateLimiter.Memory(),
                new QuantaRegisteredClientRepository(properties, new BCryptPasswordEncoder()), users);
    }

    @Test
    void unregisteredRedirectIsRejectedLocally()
    {
        Map<String, List<String>> params = authorizeParams();
        params.put("redirect_uri", List.of("https://evil.example/steal"));
        QuantaOAuthResponse response = flow.authorize(params, null, "127.0.0.1");
        assertEquals(400, response.status);
        assertNull(response.redirect);
        assertFalse(response.body.contains("evil.example"));
    }

    @Test
    void approvalDoesNotIssueCodeUntilContinue() throws Exception
    {
        Started started = start();
        when(users.selectUserById(8L)).thenReturn(member(8L, "1", "0", "0", "张三", "zhang"));
        QuantaOAuthResponse approved = flow.decide(started.id, started.browser, started.csrf,
                "https://www.quantacenter.com", login(8L), 8L, mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        assertEquals(200, approved.status);
        assertTrue(approved.body.contains("\"status\":\"approved\""));
        assertTrue(approved.body.contains("\"continuePath\":\"/oauth2/interaction/" + started.id + "/continue\""));
        assertFalse(approved.body.contains("code="));
        assertEquals(0, codes.size());

        QuantaOAuthResponse again = flow.decide(started.id, started.browser, started.csrf,
                "https://www.quantacenter.com", login(8L), 8L, mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        assertEquals(200, again.status);

        when(users.selectUserById(9L)).thenReturn(member(9L, "1", "0", "0", "另一位", "other"));
        QuantaOAuthResponse conflict = flow.decide(started.id, started.browser, started.csrf,
                "https://www.quantacenter.com", login(9L), 9L, mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        assertEquals(409, conflict.status);
    }

    @Test
    void nonMemberAndPlainAdminCannotApprove() throws Exception
    {
        Started started = start();
        when(users.selectUserById(1L)).thenReturn(member(1L, "0", "0", "0", "管理员", "admin"));
        QuantaOAuthResponse rejected = flow.decide(started.id, started.browser, started.csrf,
                "https://www.quantacenter.com", login(1L), 1L, mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        assertEquals(403, rejected.status);
        assertEquals(0, codes.size());

        when(users.selectUserById(8L)).thenReturn(member(8L, "1", "1", "0", "停用", "disabled"));
        QuantaOAuthResponse disabled = flow.decide(started.id, started.browser, started.csrf,
                "https://www.quantacenter.com", login(8L), 8L, mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        assertEquals(403, disabled.status);
    }

    @Test
    void continueIssuesCodeAndTokenRejectsReplayWrongVerifierAndNonMemberClaim() throws Exception
    {
        Started started = start();
        when(users.selectUserById(8L)).thenReturn(member(8L, "1", "0", "0", "张三", "zhang"));
        flow.decide(started.id, started.browser, started.csrf, "https://www.quantacenter.com", login(8L), 8L,
                mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        QuantaOAuthResponse back = flow.continueLogin(started.id, started.browser, 8L, "127.0.0.1");
        assertEquals(303, back.status);
        URI location = URI.create(back.redirect);
        assertEquals("https://challenge.quantacenter.com/api/auth/quanta/callback", location.getScheme() + "://"
                + location.getHost() + location.getPath());
        String code = query(location, "code");
        assertEquals("state-value-1", query(location, "state"));
        assertEquals(1, codes.size());

        QuantaOAuthResponse replayedContinue = flow.continueLogin(started.id, started.browser, 8L, "127.0.0.1");
        assertEquals(403, replayedContinue.status);
        assertNull(replayedContinue.redirect);

        Map<String, List<String>> wrong = tokenParams(code, "wrong-verifier-wrong-verifier-wrong-verifier");
        QuantaOAuthResponse rejected = flow.token(wrong, basic(), "127.0.0.1");
        assertEquals(400, rejected.status);
        assertTrue(rejected.body.contains("invalid_grant"));
        assertEquals(0, codes.size());

        Started second = start();
        flow.decide(second.id, second.browser, second.csrf, "https://www.quantacenter.com", login(8L), 8L,
                mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        QuantaOAuthResponse again = flow.continueLogin(second.id, second.browser, 8L, "127.0.0.1");
        String secondCode = query(URI.create(again.redirect), "code");
        QuantaOAuthResponse swapped = flow.token(tokenParams(secondCode, "not-the-original-verifier-not-the-original"),
                basic(), "127.0.0.1");
        assertTrue(swapped.body.contains("invalid_grant"));

        Started third = start();
        flow.decide(third.id, third.browser, third.csrf, "https://www.quantacenter.com", login(8L), 8L,
                mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        String goodCode = query(URI.create(flow.continueLogin(third.id, third.browser, 8L, "127.0.0.1").redirect), "code");
        QuantaOAuthResponse tokens = flow.token(tokenParams(goodCode, third.verifier), basic(), "127.0.0.1");
        assertEquals(200, tokens.status);
        JsonNode body = mapper.readTree(tokens.body);
        assertFalse(body.has("refresh_token"));
        SignedJWT idToken = SignedJWT.parse(body.get("id_token").asText());
        RSAKey key = (RSAKey) JWKSet.parse(flow.jwks().body).getKeys().get(0);
        assertTrue(idToken.verify(new RSASSAVerifier(key.toRSAPublicKey())));
        assertEquals("https://www.quantacenter.com", idToken.getJWTClaimsSet().getIssuer());
        assertEquals("8", idToken.getJWTClaimsSet().getSubject());
        assertEquals(List.of("quanta-challenge"), idToken.getJWTClaimsSet().getAudience());
        assertEquals("nonce-value-1", idToken.getJWTClaimsSet().getStringClaim("nonce"));
        assertEquals("张三", idToken.getJWTClaimsSet().getStringClaim("name"));
        assertEquals(Boolean.TRUE, idToken.getJWTClaimsSet().getClaim("quanta_member"));
        assertEquals("openid profile", body.get("scope").asText());
        assertNull(idToken.getJWTClaimsSet().getClaim("email"));
        assertNull(idToken.getJWTClaimsSet().getClaim("email_verified"));
        assertNull(idToken.getJWTClaimsSet().getClaim("picture"));
        QuantaOAuthResponse replay = flow.token(tokenParams(goodCode, third.verifier), basic(), "127.0.0.1");
        assertEquals(400, replay.status);

        QuantaOAuthResponse otherBrowser = flow.read(third.id, "another-browser-id-that-is-long-enough-123456", "127.0.0.1");
        assertEquals(403, otherBrowser.status);
        assertFalse(flow.read(third.id, third.browser, "127.0.0.1").body.contains(third.challenge));
    }

    @Test
    void pkceRejectsPlainAndAcceptsS256() throws Exception
    {
        String verifier = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        assertTrue(QuantaOAuthFlow.pkce(verifier, challenge));
        assertFalse(QuantaOAuthFlow.pkce(verifier, verifier));
    }

    @Test
    void profileRequestKeepsEmailPrivateAndPublishesAvatar() throws Exception
    {
        Started started = start("openid profile");
        SysUser user = member(8L, "1", "0", "0", "张三", "zhang");
        user.setEmail("member@example.test");
        user.setAvatar("/profile/avatar/2026/10/22/example.png");
        when(users.selectUserById(8L)).thenReturn(user);
        JsonNode read = mapper.readTree(flow.read(started.id, started.browser, "127.0.0.1").body);
        assertEquals(List.of("openid", "profile"), mapper.convertValue(read.get("data").get("scopes"), List.class));
        assertFalse(read.get("data").has("email"));
        assertFalse(read.get("data").toString().contains("member@example.test"));
        SignedJWT idToken = approvedToken(started, user);
        assertEquals("https://www.quantacenter.com/profile/avatar/2026/10/22/example.png",
                idToken.getJWTClaimsSet().getStringClaim("picture"));
        assertNull(idToken.getJWTClaimsSet().getClaim("email"));
        assertNull(idToken.getJWTClaimsSet().getClaim("email_verified"));
    }

    @Test
    void emailScopeReleasesBooleanVerifiedFalseAndOmitsEmptyOrUnsafeClaims() throws Exception
    {
        Started started = start("email openid profile");
        SysUser user = member(8L, "1", "0", "0", "张三", "zhang");
        user.setEmail("member@example.test");
        user.setAvatar("https://www.quantacenter.com/profile/avatar/example.webp");
        SignedJWT idToken = approvedToken(started, user);
        assertEquals("openid profile email", mapper.readTree(lastTokenBody).get("scope").asText());
        assertEquals("member@example.test", idToken.getJWTClaimsSet().getStringClaim("email"));
        assertEquals(Boolean.FALSE, idToken.getJWTClaimsSet().getClaim("email_verified"));
        assertEquals("https://www.quantacenter.com/profile/avatar/example.webp",
                idToken.getJWTClaimsSet().getStringClaim("picture"));
        SignedJWT access = SignedJWT.parse(mapper.readTree(lastTokenBody).get("access_token").asText());
        assertNull(access.getJWTClaimsSet().getClaim("email"));
        assertNull(access.getJWTClaimsSet().getClaim("picture"));

        Started bare = start("openid profile email");
        SysUser withoutEmail = member(8L, "1", "0", "0", "张三", "zhang");
        withoutEmail.setAvatar("javascript:alert(1)");
        SignedJWT omitted = approvedToken(bare, withoutEmail);
        assertEquals("openid profile email", mapper.readTree(lastTokenBody).get("scope").asText());
        assertNull(omitted.getJWTClaimsSet().getClaim("email"));
        assertNull(omitted.getJWTClaimsSet().getClaim("email_verified"));
        assertNull(omitted.getJWTClaimsSet().getClaim("picture"));
    }

    @Test
    void invalidScopesAreRejectedWithoutDowngrade()
    {
        for (String scope : List.of("openid", "openid profile email email", "openid profile phone", "OpenID profile",
                "openid profile email ", "openid  profile"))
        {
            Map<String, List<String>> params = authorizeParams(scope);
            QuantaOAuthResponse response = flow.authorize(params, null, "127.0.0.1");
            assertEquals(400, response.status, scope);
            assertNull(response.redirect);
        }
    }

    @Test
    void decisionAndTokenCannotExpandScope() throws Exception
    {
        Started started = start("openid profile");
        SysUser user = member(8L, "1", "0", "0", "张三", "zhang");
        user.setEmail("member@example.test");
        when(users.selectUserById(8L)).thenReturn(user);
        QuantaOAuthResponse expanded = flow.decide(started.id, started.browser, started.csrf,
                "https://www.quantacenter.com", login(8L), 8L,
                mapper.readTree("{\"action\":\"approve\",\"scopes\":[\"email\"]}"), "127.0.0.1");
        assertEquals(400, expanded.status);

        flow.decide(started.id, started.browser, started.csrf, "https://www.quantacenter.com", login(8L), 8L,
                mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        String code = query(URI.create(flow.continueLogin(started.id, started.browser, 8L, "127.0.0.1").redirect), "code");
        Map<String, List<String>> widened = tokenParams(code, started.verifier);
        widened.put("scope", List.of("openid profile email"));
        QuantaOAuthResponse rejected = flow.token(widened, basic(), "127.0.0.1");
        assertEquals(400, rejected.status);
        assertTrue(rejected.body.contains("invalid_request"));
        assertEquals(1, codes.size());
        SignedJWT idToken = SignedJWT.parse(mapper.readTree(flow.token(tokenParams(code, started.verifier), basic(), "127.0.0.1").body)
                .get("id_token").asText());
        assertNull(idToken.getJWTClaimsSet().getClaim("email"));
    }

    @Test
    void legacyPayloadsDoNotGrantEmail() throws Exception
    {
        long future = System.currentTimeMillis() + 60_000L;
        String legacy = String.join("\n", "quanta-challenge", properties.getRedirectUri(), "8", "nonce-value-1",
                "c".repeat(43), Long.toString(future));
        assertEquals("openid profile", RedisQuantaOAuthCodeStore.decode(legacy, System.currentTimeMillis()).scopes);
        assertEquals("openid profile email",
                RedisQuantaOAuthCodeStore.decode(legacy + "\nopenid profile email", System.currentTimeMillis()).scopes);
        assertEquals("openid profile",
                RedisQuantaOAuthCodeStore.decode(legacy + "\nemail", System.currentTimeMillis()).scopes);

        QuantaOAuthInteraction interaction = new QuantaOAuthInteraction();
        interaction.id = "a".repeat(43);
        interaction.browserId = "b".repeat(43);
        interaction.csrfToken = "d".repeat(43);
        interaction.status = "pending";
        interaction.expiresAt = future;
        interaction.scopes = null;
        interactions.create(interaction);
        JsonNode data = mapper.readTree(flow.read(interaction.id, interaction.browserId, "127.0.0.1").body).get("data");
        assertEquals(List.of("openid", "profile"), mapper.convertValue(data.get("scopes"), List.class));
        assertFalse(data.toString().contains("email"));

        Started started = start();
        QuantaOAuthCode stored = new QuantaOAuthCode();
        stored.clientId = properties.getClientId();
        stored.redirectUri = properties.getRedirectUri();
        stored.userId = "8";
        stored.nonce = "nonce-value-1";
        stored.codeChallenge = started.challenge;
        stored.expiresAt = future;
        stored.scopes = null;
        codes.save("legacy-code", stored);
        SysUser user = member(8L, "1", "0", "0", "张三", "zhang");
        user.setEmail("member@example.test");
        when(users.selectUserById(8L)).thenReturn(user);
        JsonNode body = mapper.readTree(flow.token(tokenParams("legacy-code", started.verifier), basic(), "127.0.0.1").body);
        assertEquals("openid profile", body.get("scope").asText());
        assertNull(SignedJWT.parse(body.get("id_token").asText()).getJWTClaimsSet().getClaim("email"));
    }

    @Test
    void discoveryAdvertisesEmailClaimsWithoutUserInfo() throws Exception
    {
        JsonNode document = mapper.readTree(flow.discovery().body);
        assertEquals(List.of("openid", "profile", "email"),
                mapper.convertValue(document.get("scopes_supported"), List.class));
        assertEquals(List.of("iss", "sub", "aud", "iat", "exp", "nonce", "name", "picture", "email", "email_verified",
                "quanta_member"), mapper.convertValue(document.get("claims_supported"), List.class));
        assertFalse(document.has("userinfo_endpoint"));
        assertEquals("https://www.quantacenter.com/oauth2/token", document.get("token_endpoint").asText());
        assertEquals("RS256", document.get("id_token_signing_alg_values_supported").get(0).asText());
    }

    @Test
    void pictureUrlRejectsPrivateAndCredentialedAddresses()
    {
        String origin = "https://www.quantacenter.com";
        assertEquals(origin + "/profile/avatar/a.jpg", QuantaOAuthFlow.pictureUrl("/profile/avatar/a.jpg", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("/profile/upload/qt/forum/a.png", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("https://evil.example/profile/avatar/a.png", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("https://user:pass@www.quantacenter.com/profile/avatar/a.png", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("http://www.quantacenter.com/profile/avatar/a.png", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("/profile/avatar/../secret.png", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("/profile/avatar/a.png?token=secret", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("data:image/png;base64,aaaa", origin));
        assertNull(QuantaOAuthFlow.pictureUrl("", origin));
    }

    @Test
    void emailVerifiedSerializesAsBooleanAndStaysUnsetUntilGetInfo() throws Exception
    {
        SysUser user = new SysUser();
        user.setEmailVerified(Boolean.FALSE);
        JsonNode node = mapper.readTree(mapper.writeValueAsString(user));
        assertTrue(node.get("emailVerified").isBoolean());
        assertFalse(node.get("emailVerified").booleanValue());
        assertFalse(mapper.readTree(mapper.writeValueAsString(new SysUser())).has("emailVerified"));
    }

    @Test
    void displayNameUsesNickNameThenUserName()
    {
        SysUser named = member(3L, "1", "0", "0", "实名", "account");
        assertEquals("实名", QuantaOAuthFlow.displayName(named));
        named.setNickName("");
        assertEquals("account", QuantaOAuthFlow.displayName(named));
        assertFalse(QuantaOAuthFlow.member(member(3L, "0", "0", "0", "实名", "account")));
        assertFalse(QuantaOAuthFlow.member(member(3L, "1", "1", "0", "实名", "account")));
        assertFalse(QuantaOAuthFlow.member(member(3L, "1", "0", "2", "实名", "account")));
    }

    private Started start() throws Exception
    {
        return start("openid profile");
    }

    private Started start(String scope) throws Exception
    {
        QuantaOAuthResponse response = flow.authorize(authorizeParams(scope), null, "127.0.0.1");
        assertEquals(302, response.status, scope);
        assertTrue(response.redirect.startsWith("https://www.quantacenter.com/oauth/authorize?interaction="));
        Started started = new Started();
        started.id = URI.create(response.redirect).getQuery().substring("interaction=".length());
        started.browser = response.browserCookie;
        started.verifier = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(started.verifier.getBytes(StandardCharsets.US_ASCII));
        started.challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        JsonNode body = mapper.readTree(flow.read(started.id, started.browser, "127.0.0.1").body);
        started.csrf = body.get("data").get("csrfToken").asText();
        assertFalse(body.toString().contains(started.challenge));
        return started;
    }

    private SignedJWT approvedToken(Started started, SysUser user) throws Exception
    {
        when(users.selectUserById(user.getUserId())).thenReturn(user);
        flow.decide(started.id, started.browser, started.csrf, "https://www.quantacenter.com", login(user.getUserId()),
                user.getUserId(), mapper.readTree("{\"action\":\"approve\"}"), "127.0.0.1");
        String code = query(URI.create(flow.continueLogin(started.id, started.browser, user.getUserId(), "127.0.0.1").redirect),
                "code");
        QuantaOAuthResponse tokens = flow.token(tokenParams(code, started.verifier), basic(), "127.0.0.1");
        assertEquals(200, tokens.status);
        lastTokenBody = tokens.body;
        return SignedJWT.parse(mapper.readTree(tokens.body).get("id_token").asText());
    }

    private Map<String, List<String>> authorizeParams()
    {
        return authorizeParams("openid profile");
    }

    private Map<String, List<String>> authorizeParams(String scope)
    {
        String verifier = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String challenge;
        try
        {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException(ex);
        }
        Map<String, List<String>> params = new java.util.LinkedHashMap<>();
        params.put("response_type", List.of("code"));
        params.put("client_id", List.of("quanta-challenge"));
        params.put("redirect_uri", List.of(properties.getRedirectUri()));
        params.put("scope", List.of(scope));
        params.put("state", List.of("state-value-1"));
        params.put("nonce", List.of("nonce-value-1"));
        params.put("code_challenge", List.of(challenge));
        params.put("code_challenge_method", List.of("S256"));
        return params;
    }

    private Map<String, List<String>> tokenParams(String code, String verifier)
    {
        Map<String, List<String>> params = new java.util.LinkedHashMap<>();
        params.put("grant_type", List.of("authorization_code"));
        params.put("code", List.of(code));
        params.put("redirect_uri", List.of(properties.getRedirectUri()));
        params.put("code_verifier", List.of(verifier));
        return params;
    }

    private String basic()
    {
        String raw = "quanta-challenge:" + properties.getClientSecret();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String query(URI uri, String name)
    {
        for (String pair : uri.getQuery().split("&"))
        {
            String[] parts = pair.split("=", 2);
            if (name.equals(parts[0]))
            {
                return java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static LoginUser login(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        return new LoginUser(userId, 1L, user, Set.of());
    }

    private static SysUser member(Long id, String quanta, String status, String deleted, String nick, String account)
    {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setIsQuantaMember(quanta);
        user.setStatus(status);
        user.setDelFlag(deleted);
        user.setNickName(nick);
        user.setUserName(account);
        return user;
    }

    private static final class Started
    {
        private String id;
        private String browser;
        private String csrf;
        private String verifier;
        private String challenge;
    }
}
