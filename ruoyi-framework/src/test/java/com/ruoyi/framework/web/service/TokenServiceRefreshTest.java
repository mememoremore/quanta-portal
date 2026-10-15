package com.ruoyi.framework.web.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenServiceRefreshTest
{
    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private TokenService tokenService;

    private final Map<String, Object> data = new HashMap<>();

    private final Map<String, TimeUnit> units = new HashMap<>();

    private final Map<String, Integer> amounts = new HashMap<>();

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(tokenService, "expireTime", 15);
        ReflectionTestUtils.setField(tokenService, "refreshCookieName", "quanta_refresh");
        ReflectionTestUtils.setField(tokenService, "refreshSecure", false);
        ReflectionTestUtils.setField(tokenService, "refreshMemberExpireDays", 7);
        ReflectionTestUtils.setField(tokenService, "refreshAdminExpireDays", 1);
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            data.put(key, invocation.getArgument(1));
            amounts.put(key, invocation.getArgument(2));
            units.put(key, invocation.getArgument(3));
            return null;
        }).when(redisCache).setCacheObject(anyString(), any(), anyInt(), any(TimeUnit.class));
        when(redisCache.getCacheObject(anyString())).thenAnswer(invocation -> data.get(invocation.getArgument(0)));
        when(redisCache.deleteObject(anyString())).thenAnswer(invocation -> data.remove(invocation.getArgument(0)) != null);
    }

    @Test
    void rotatesRefreshAndRejectsReplayByRevokingTheUser()
    {
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        tokenService.issueRefresh(firstResponse, loginUser(null, Set.of()), null);
        String firstToken = cookieValue(firstResponse);

        RefreshSession session = tokenService.consumeRefresh(requestWith(firstToken), new MockHttpServletResponse());
        assertNotNull(session);
        assertEquals(7L, session.getUserId());
        assertEquals("alice", session.getUsername());

        MockHttpServletResponse rotated = new MockHttpServletResponse();
        tokenService.issueRefresh(rotated, loginUser(null, Set.of()), session.getFamilyId());
        assertNotNull(cookieValue(rotated));

        RefreshSession replay = tokenService.consumeRefresh(requestWith(firstToken), new MockHttpServletResponse());
        assertNull(replay);
        assertFalse(data.keySet().stream().anyMatch(key -> key.startsWith(CacheConstants.REFRESH_TOKEN_KEY)));
    }

    @Test
    void refreshTtlFollowsServerRole()
    {
        tokenService.issueRefresh(new MockHttpServletResponse(), loginUser(null, Set.of()), null);
        assertEquals(7, refreshAmount());
        assertEquals(TimeUnit.DAYS, refreshUnit());

        data.clear();
        units.clear();
        amounts.clear();
        tokenService.issueRefresh(new MockHttpServletResponse(), loginUser("qt_manager", Set.of()), null);
        assertEquals(1, refreshAmount());

        data.clear();
        units.clear();
        amounts.clear();
        tokenService.issueRefresh(new MockHttpServletResponse(), loginUser(null, Set.of("*:*:*")), null);
        assertEquals(1, refreshAmount());
    }

    @Test
    void logoutClearsThePresentedRefreshCookie()
    {
        MockHttpServletResponse issued = new MockHttpServletResponse();
        tokenService.issueRefresh(issued, loginUser(null, Set.of()), null);
        String token = cookieValue(issued);
        MockHttpServletRequest request = requestWith(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenService.revokePresentedRefresh(request);
        tokenService.clearRefreshCookie(response);

        String cleared = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cleared);
        assertTrue(cleared.contains("Max-Age=0"));
        assertTrue(cleared.contains("HttpOnly"));
        assertNull(tokenService.consumeRefresh(requestWith(token), new MockHttpServletResponse()));
    }

    private int refreshAmount()
    {
        return amounts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(CacheConstants.REFRESH_TOKEN_KEY))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
    }

    private TimeUnit refreshUnit()
    {
        return units.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(CacheConstants.REFRESH_TOKEN_KEY))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
    }

    private static LoginUser loginUser(String roleKey, Set<String> permissions)
    {
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setUserName("alice");
        if (roleKey != null)
        {
            SysRole role = new SysRole();
            role.setRoleKey(roleKey);
            user.setRoles(List.of(role));
        }
        LoginUser loginUser = new LoginUser(7L, 1L, user, permissions);
        loginUser.setToken("access-1");
        return loginUser;
    }

    private static MockHttpServletRequest requestWith(String token)
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("quanta_refresh", token));
        return request;
    }

    private static String cookieValue(MockHttpServletResponse response)
    {
        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header);
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
        String pair = header.split(";", 2)[0];
        return pair.substring("quanta_refresh=".length());
    }
}
