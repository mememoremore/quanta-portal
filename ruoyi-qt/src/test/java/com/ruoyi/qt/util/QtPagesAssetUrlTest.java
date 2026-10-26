package com.ruoyi.qt.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QtPagesAssetUrlTest
{
    private static final String BASE = "https://www.quantacenter.com";

    @Test
    void convertsRelativeAvatarToAbsoluteUrl()
    {
        assertEquals("https://www.quantacenter.com/profile/avatar/2026/10/26/abc.png",
                QtPagesAssetUrl.toPublicUrl("/profile/avatar/2026/10/26/abc.png", BASE));
        assertEquals("https://www.quantacenter.com/profile/avatar/abc.PNG",
                QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.PNG", BASE));
        assertEquals("https://www.quantacenter.com/profile/avatar/abc.webp",
                QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.webp", BASE));
    }

    @Test
    void keepsSameOriginAbsoluteUrl()
    {
        String value = "https://www.quantacenter.com/profile/avatar/2026/10/26/abc.jpg";
        assertEquals(value, QtPagesAssetUrl.toPublicUrl(value, BASE));
    }

    @Test
    void normalizesBaseTrailingSlash()
    {
        assertEquals("https://www.quantacenter.com/profile/avatar/abc.png",
                QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.png", BASE + "/"));
    }

    @Test
    void rejectsEmptyValues()
    {
        assertEquals("", QtPagesAssetUrl.toPublicUrl(null, BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("   ", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.png", null));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.png", ""));
    }

    @Test
    void rejectsForeignAndUnsafeUrls()
    {
        assertEquals("", QtPagesAssetUrl.toPublicUrl("https://evil.example/profile/avatar/abc.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("http://www.quantacenter.com/profile/avatar/abc.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("//evil.example/profile/avatar/abc.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("javascript:alert(1)", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("data:image/png;base64,abc", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("https://www.quantacenter.com.evil.example/profile/avatar/abc.png", BASE));
    }

    @Test
    void rejectsPathTraversalAndNonAvatarPaths()
    {
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/../secret.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/%2e%2e/secret.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar//abc.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.png?x=1", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.png#frag", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc\\def.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/upload/abc.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("profile/avatar/abc.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.svg", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc.gif", BASE));
    }

    @Test
    void rejectsControlCharacters()
    {
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc\r\n.png", BASE));
        assertEquals("", QtPagesAssetUrl.toPublicUrl("/profile/avatar/abc\0.png", BASE));
    }
}
