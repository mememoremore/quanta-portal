package com.ruoyi.qt;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.framework.security.ProfileAccessSigner;
import com.ruoyi.qt.controller.QtInterviewController;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.service.IQtInterviewAdminService;
import com.ruoyi.qt.util.QtInterviewAccessUrls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtInterviewResumeAccessTest
{
    @Mock
    private ProfileAccessSigner signer;

    @Mock
    private ServerConfig serverConfig;

    @Mock
    private IQtInterviewAdminService adminService;

    @AfterEach
    void clear()
    {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void resumeSizeCapIsTenMegabytes()
    {
        assertEquals(10L * 1024 * 1024, FileValidator.SIZE_RESUME);
        assertEquals(10L * 1024 * 1024, FileValidator.SIZE_INTERVIEW_PHOTO);
    }

    @Test
    void pdfMagicIsAcceptedAndRenamedJpegIsRejected() throws Exception
    {
        MockMultipartFile pdf = new MockMultipartFile("resumeFile", "cv.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII));
        FileValidator.validate(pdf, MimeTypeUtils.PDF_EXTENSION, FileValidator.SIZE_RESUME);

        MockMultipartFile fake = new MockMultipartFile("resumeFile", "cv.pdf", "application/pdf",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 });
        assertThrows(ServiceException.class,
                () -> FileValidator.validate(fake, MimeTypeUtils.PDF_EXTENSION, FileValidator.SIZE_RESUME));
    }

    @Test
    void wechatJpegNamedWebpPhotoIsAccepted() throws Exception
    {
        MockMultipartFile file = new MockMultipartFile("photoFile", "photo.jpg", "image/jpeg", webpHeader());
        FileValidator.validate(file, MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE);
        FileUploadUtils.assertAllowed(file, MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE);
    }

    @Test
    void webpPhotoExtensionIsAccepted() throws Exception
    {
        MockMultipartFile file = new MockMultipartFile("photoFile", "photo.webp", "image/webp", webpHeader());
        FileValidator.validate(file, MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE);
        FileUploadUtils.assertAllowed(file, MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE);
    }

    @Test
    void htmlRenamedAsJpegIsStillRejected()
    {
        MockMultipartFile fake = new MockMultipartFile("photoFile", "photo.jpg", "image/jpeg",
                "<html><body>nope</body>".getBytes(StandardCharsets.US_ASCII));
        assertThrows(ServiceException.class,
                () -> FileValidator.validate(fake, MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE));
    }

    @Test
    void photoErrorMessagesAreUserFacing()
    {
        QtInterviewController controller = new QtInterviewController();
        Exception tooBig = new ServiceException("文件大小超出限制，最大允许 10MB");
        assertEquals("证件照不能超过 10 MB，请压缩后重试",
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(controller, "photoUploadMessage",
                        tooBig));
        Exception resumeTooBig = new ServiceException("文件大小超出限制，最大允许 10MB");
        assertEquals("PDF 简历不能超过 10 MB，请压缩后重试",
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(controller, "resumeUploadMessage",
                        resumeTooBig));
        Exception notPdf = new com.ruoyi.common.exception.file.InvalidExtensionException(
                MimeTypeUtils.PDF_EXTENSION, "docx", "cv.docx");
        assertEquals("简历必须是 PDF 文件",
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(controller, "resumeUploadMessage",
                        notPdf));
    }

    private static byte[] webpHeader()
    {
        byte[] webp = new byte[16];
        webp[0] = 'R';
        webp[1] = 'I';
        webp[2] = 'F';
        webp[3] = 'F';
        webp[8] = 'W';
        webp[9] = 'E';
        webp[10] = 'B';
        webp[11] = 'P';
        return webp;
    }

    @Test
    void fillSignsRelativeResumePath()
    {
        when(serverConfig.getUrl()).thenReturn("https://www.quantacenter.com");
        when(signer.signUrl("https://www.quantacenter.com/profile/upload/qt/interview-resume/a.pdf"))
                .thenReturn("https://www.quantacenter.com/profile/upload/qt/interview-resume/a.pdf?sig=1");
        QtInterviewApplication application = new QtInterviewApplication();
        application.setResumeUrl("/profile/upload/qt/interview-resume/a.pdf");
        application.setResumeFileName("cv.pdf");
        QtInterviewAccessUrls.fill(application, signer, serverConfig);
        assertEquals("https://www.quantacenter.com/profile/upload/qt/interview-resume/a.pdf?sig=1",
                application.getResumeAccessUrl());
    }

    @Test
    void memberListStripsResumeFields()
    {
        QtInterviewApplication row = new QtInterviewApplication();
        row.setPhonenumber("13800000000");
        row.setPhotoUrl("/profile/upload/qt/interview-photo/a.jpg");
        row.setPhotoAccessUrl("https://example.com/a.jpg?sig=1");
        row.setResumeUrl("/profile/upload/qt/interview-resume/a.pdf");
        row.setResumeAccessUrl("https://example.com/a.pdf?sig=1");
        row.setResumeFileName("cv.pdf");
        when(adminService.selectMemberList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(row));

        QtInterviewController controller = new QtInterviewController();
        ReflectionTestUtils.setField(controller, "qtInterviewAdminService", adminService);
        loginMember();
        controller.memberApplications(new QtInterviewApplication());

        assertNull(row.getPhonenumber());
        assertNull(row.getPhotoUrl());
        assertNull(row.getPhotoAccessUrl());
        assertNull(row.getResumeUrl());
        assertNull(row.getResumeAccessUrl());
        assertNull(row.getResumeFileName());
    }

    private void loginMember()
    {
        com.ruoyi.common.core.domain.entity.SysUser user = new com.ruoyi.common.core.domain.entity.SysUser();
        user.setUserId(8L);
        user.setUserName("tower_a");
        user.setIsQuantaMember("1");
        com.ruoyi.common.core.domain.entity.SysRole role = new com.ruoyi.common.core.domain.entity.SysRole();
        role.setRoleKey("qt_member");
        user.setRoles(java.util.List.of(role));
        com.ruoyi.common.core.domain.model.LoginUser loginUser =
                new com.ruoyi.common.core.domain.model.LoginUser(8L, 100L, user, java.util.Set.of());
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(loginUser, null,
                        java.util.Collections.emptyList());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
