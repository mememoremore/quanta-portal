package com.ruoyi.web.controller.system;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.framework.security.TrustedPortalOriginPolicy;
import com.ruoyi.framework.web.service.RefreshSession;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthRefreshControllerOriginTest
{
    @Mock
    private TokenService tokenService;

    @Mock
    private ISysUserService userService;

    @Mock
    private SysPermissionService permissionService;

    @InjectMocks
    private AuthRefreshController controller;

    private final TrustedPortalOriginPolicy policy = new TrustedPortalOriginPolicy();

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(policy, "allowedOrigins", "https://www.quantacenter.com");
        ReflectionTestUtils.setField(policy, "pagesEnabled", true);
        ReflectionTestUtils.setField(policy, "pagesBaseDomain", "page.quantacenter.com");
        ReflectionTestUtils.setField(controller, "trustedPortalOriginPolicy", policy);
    }

    @Test
    void rejectsPagesOriginBeforeConsumingRefresh()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://alice.page.quantacenter.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AjaxResult result = controller.refresh(request, response);

        assertEquals(403, response.getStatus());
        assertEquals(403, result.get("code"));
        verify(tokenService, never()).consumeRefresh(any(), any());
    }

    @Test
    void rejectsUnknownOriginBeforeConsumingRefresh()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AjaxResult result = controller.refresh(request, response);

        assertEquals(403, response.getStatus());
        assertEquals(403, result.get("code"));
        verify(tokenService, never()).consumeRefresh(any(), any());
    }

    @Test
    void rejectsMissingOriginAndReferer()
    {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.refresh(new MockHttpServletRequest(), response);

        assertEquals(403, response.getStatus());
        verify(tokenService, never()).consumeRefresh(any(), any());
    }

    @Test
    void rejectsLiteralNullOrigin()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "null");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.refresh(request, response);

        assertEquals(403, response.getStatus());
        verify(tokenService, never()).consumeRefresh(any(), any());
    }

    @Test
    void acceptsNoOriginWithTrustedReferer()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://www.quantacenter.com/member/pages");
        stubSuccessfulRefresh();

        controller.refresh(request, new MockHttpServletResponse());

        verify(tokenService).consumeRefresh(any(), any());
    }

    @Test
    void acceptsTrustedPortalOriginAndRotatesSession()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://www.quantacenter.com");
        stubSuccessfulRefresh();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AjaxResult result = controller.refresh(request, response);

        verify(tokenService).consumeRefresh(request, response);
        verify(tokenService).issueRefresh(any(), any(), any());
        assertNotNull(result.get("token"));
    }

    private void stubSuccessfulRefresh()
    {
        RefreshSession session = new RefreshSession();
        session.setUserId(7L);
        session.setUsername("alice");
        session.setFamilyId("family-1");
        when(tokenService.consumeRefresh(any(), any())).thenReturn(session);

        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setUserName("alice");
        when(userService.selectUserByUserName("alice")).thenReturn(user);
        when(permissionService.getMenuPermission(user)).thenReturn(Set.of());
        when(tokenService.createToken(any())).thenReturn("access-token");
    }
}
