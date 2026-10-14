package com.ruoyi.framework.security.filter;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.security.ProfileAccessSigner;
import com.ruoyi.framework.web.service.TokenService;
import jakarta.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAccessFilterTest
{
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private ProfileAccessSigner signer;

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sensitivePathRejectsAuthenticatedSessionWithoutSignature() throws Exception
    {
        loginMember();
        ProfileAccessFilter filter = new ProfileAccessFilter(signer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/upload/qt/payment-proof/p.png");
        request.setRequestURI("/profile/upload/qt/payment-proof/p.png");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void sensitivePathAllowsValidSignature() throws Exception
    {
        when(tokenService.getSigningSecret()).thenReturn("unit-test-secret-key-32bytes-min!!");
        String path = "/profile/upload/qt/payment-proof/p.png";
        String signed = signer.signUrl("https://example.com" + path);
        ProfileAccessFilter filter = new ProfileAccessFilter(signer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        int queryIndex = signed.indexOf('?');
        for (String pair : signed.substring(queryIndex + 1).split("&"))
        {
            String[] parts = pair.split("=", 2);
            request.setParameter(parts[0], parts[1]);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void catalogImagesStayAnonymous() throws Exception
    {
        ProfileAccessFilter filter = new ProfileAccessFilter(signer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/upload/qt/clothing-item/a.png");
        request.setRequestURI("/profile/upload/qt/clothing-item/a.png");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
    }

    private void loginMember()
    {
        SysUser user = new SysUser();
        user.setUserId(8L);
        user.setUserName("tower_a");
        user.setIsQuantaMember("1");
        LoginUser loginUser = new LoginUser(8L, 100L, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
