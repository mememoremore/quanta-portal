package com.ruoyi.framework.security;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustedPortalOriginPolicyTest
{
    private final TrustedPortalOriginPolicy policy = new TrustedPortalOriginPolicy();

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(policy, "allowedOrigins", "https://www.quantacenter.com");
        ReflectionTestUtils.setField(policy, "pagesEnabled", true);
        ReflectionTestUtils.setField(policy, "pagesBaseDomain", "page.quantacenter.com");
    }

    @Test
    void pagesOriginIsNeverTrusted()
    {
        assertFalse(policy.isTrustedOrigin("https://alice.page.quantacenter.com"));
        assertFalse(policy.isTrustedOrigin("https://page.quantacenter.com"));
    }

    @Test
    void exactPortalOriginIsTrusted()
    {
        assertTrue(policy.isTrustedOrigin("https://www.quantacenter.com"));
        assertFalse(policy.isTrustedOrigin("https://www.quantacenter.com.evil.example"));
        assertFalse(policy.isTrustedOrigin("https://evil.example"));
    }

    @Test
    void pagesOriginRejectedEvenWhenPagesDisabledWithWildcardFallback()
    {
        ReflectionTestUtils.setField(policy, "pagesEnabled", false);
        ReflectionTestUtils.setField(policy, "allowedOrigins", "*");
        assertTrue(policy.isTrustedOrigin("https://www.quantacenter.com"));
        assertFalse(policy.isTrustedOrigin("https://alice.page.quantacenter.com"));
    }

    @Test
    void wildcardFallbackStillTrustedWhenPagesDisabled()
    {
        ReflectionTestUtils.setField(policy, "pagesEnabled", false);
        ReflectionTestUtils.setField(policy, "allowedOrigins", "*");
        assertTrue(policy.isTrustedOrigin("https://localhost:5173"));
    }

    @Test
    void wildcardIsNeverTrustedWhenPagesEnabled()
    {
        ReflectionTestUtils.setField(policy, "allowedOrigins", "*");
        assertFalse(policy.isTrustedOrigin("https://anything.example"));
    }

    @Test
    void pagesEnabledRejectsWildcardOrigins()
    {
        ReflectionTestUtils.setField(policy, "allowedOrigins", "*");
        assertThrows(IllegalStateException.class, policy::assertStrictOriginsForPages);

        ReflectionTestUtils.setField(policy, "allowedOrigins", "https://*.quantacenter.com");
        assertThrows(IllegalStateException.class, policy::assertStrictOriginsForPages);

        ReflectionTestUtils.setField(policy, "allowedOrigins", " , ");
        assertThrows(IllegalStateException.class, policy::assertStrictOriginsForPages);
    }

    @Test
    void pagesEnabledAcceptsExactOriginList()
    {
        ReflectionTestUtils.setField(policy, "allowedOrigins",
                "https://www.quantacenter.com, https://portal.quantacenter.com");
        policy.assertStrictOriginsForPages();
        assertEquals(List.of("https://www.quantacenter.com", "https://portal.quantacenter.com"), policy.exactOrigins());
    }

    @Test
    void disabledPagesSkipsStrictValidation()
    {
        ReflectionTestUtils.setField(policy, "pagesEnabled", false);
        ReflectionTestUtils.setField(policy, "allowedOrigins", "*");
        policy.assertStrictOriginsForPages();
    }

    @Test
    void assertRefreshAllowedThrowsForUntrustedOrigin()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://alice.page.quantacenter.com");
        ServiceException exception = assertThrows(ServiceException.class, () -> policy.assertRefreshAllowed(request));
        assertEquals(403, exception.getCode());
    }

    @Test
    void refererFallbackAcceptsTrustedPortalOnly()
    {
        MockHttpServletRequest trusted = new MockHttpServletRequest();
        trusted.addHeader("Referer", "https://www.quantacenter.com/member/pages");
        assertTrue(policy.isRefreshAllowed(trusted));

        MockHttpServletRequest pages = new MockHttpServletRequest();
        pages.addHeader("Referer", "https://alice.page.quantacenter.com/");
        assertFalse(policy.isRefreshAllowed(pages));
    }

    @Test
    void pagesOriginPatternsCoverBareAndWildcardSubdomains()
    {
        assertEquals(List.of("https://page.quantacenter.com", "https://*.page.quantacenter.com"),
                policy.pagesOriginPatterns());

        ReflectionTestUtils.setField(policy, "pagesBaseDomain", " ");
        assertEquals(List.of("https://page.quantacenter.com", "https://*.page.quantacenter.com"),
                policy.pagesOriginPatterns());
    }
}
