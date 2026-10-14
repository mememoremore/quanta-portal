package com.ruoyi.framework.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.framework.web.service.TokenService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAccessSignerTest
{
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private ProfileAccessSigner signer;

    @Test
    void catalogImagesStayPublic()
    {
        assertTrue(signer.isPublicResource("/profile/upload/qt/clothing-item/a.png"));
        assertTrue(signer.isPublicResource("/profile/upload/qt/payment-qr/a.png"));
        assertFalse(signer.isSensitiveResource("/profile/upload/qt/clothing-item/a.png"));
    }

    @Test
    void paymentProofIsSensitiveAndSignable()
    {
        when(tokenService.getSigningSecret()).thenReturn("unit-test-secret-key-32bytes-min!!");
        String path = "/profile/upload/qt/payment-proof/p.png";
        assertTrue(signer.isSensitiveResource(path));
        String signed = signer.signUrl("https://example.com" + path);
        assertTrue(signed.contains("sig="));
        assertTrue(signed.contains("exp="));
    }

    @Test
    void interviewResumeIsSensitiveAndSignable()
    {
        when(tokenService.getSigningSecret()).thenReturn("unit-test-secret-key-32bytes-min!!");
        String path = "/profile/upload/qt/interview-resume/2026/09/17/a.pdf";
        assertTrue(signer.isSensitiveResource(path));
        String signed = signer.signUrl("https://example.com" + path);
        assertTrue(signed.contains("sig="));
    }

    @Test
    void downloadBypassPathsAreSensitive()
    {
        assertTrue(signer.isSensitiveDownloadResource("/profile/upload/qt/payment-proof/p.png"));
        assertTrue(signer.isSensitiveDownloadResource("/profile/upload/qt/interview-resume/a.pdf"));
        assertTrue(signer.isSensitiveDownloadResource("upload/qt/materials/a.pdf"));
        assertTrue(signer.isSensitiveDownloadResource("/profile/upload/qt/interview-photo/a.jpg"));
        assertFalse(signer.isSensitiveDownloadResource("/profile/upload/qt/clothing-item/a.png"));
        assertFalse(signer.isSensitiveDownloadResource("/profile/upload/qt/payment-qr/a.png"));
    }
}
