package com.ruoyi.qt.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtForumImage;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.mapper.QtForumImageMapper;
import com.ruoyi.qt.mapper.QtForumPostMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QtForumImageServiceTest
{
    private static final String ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    @TempDir Path directory;
    @Mock QtForumImageMapper imageMapper;
    @Mock QtForumPostMapper postMapper;
    @InjectMocks QtForumImageService service;
    private String originalProfile;

    @BeforeEach void setup()
    {
        originalProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(directory.toString());
        login(8L);
    }
    @AfterEach void clear()
    {
        new RuoYiConfig().setProfile(originalProfile);
        SecurityContextHolder.clearContext();
    }

    @Test void draftIsOnlyReadableByUploaderAndCannotBeStolen()
    {
        QtForumImage image = image();
        when(imageMapper.selectImage(ID)).thenReturn(image);
        assertSame(image, service.readableImage(ID));
        login(9L);
        assertEquals(404, assertThrows(ServiceException.class, () -> service.readableImage(ID)).getCode());
        when(imageMapper.lockImage(ID)).thenReturn(image);
        assertThrows(ServiceException.class, () -> service.bindImages(source(), 5L));
        verify(imageMapper, never()).bindImage(any(), any());
    }

    @Test void publishedImageObeysCurrentPostScopeEvenForUploader()
    {
        QtForumImage image = image(); image.setPostId(5L);
        QtForumPost post = new QtForumPost(); post.setVisibility("MANAGERS"); post.setContent(source());
        when(imageMapper.selectImage(ID)).thenReturn(image);
        when(postMapper.selectPostById(5L)).thenReturn(post);
        when(postMapper.canManagePosts(8L)).thenReturn(true, false);
        assertSame(image, service.readableImage(ID));
        assertEquals(404, assertThrows(ServiceException.class, () -> service.readableImage(ID)).getCode());
        when(postMapper.selectPostById(5L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.readableImage(ID));
    }

    @Test void removedImagesAndImagesBoundToOtherPostsAreNotReused()
    {
        QtForumImage image = image(); image.setPostId(5L);
        when(imageMapper.lockImage(ID)).thenReturn(image);
        assertThrows(ServiceException.class, () -> service.bindImages(source(), 6L));
        when(imageMapper.selectImage(ID)).thenReturn(image);
        QtForumPost post = new QtForumPost(); post.setVisibility("MEMBERS"); post.setContent("图片已移除");
        when(postMapper.selectPostById(5L)).thenReturn(post);
        assertThrows(ServiceException.class, () -> service.readableImage(ID));
    }

    @Test void validOwnedImageBindsOncePerUniqueResource()
    {
        when(imageMapper.lockImage(ID)).thenReturn(image());
        when(imageMapper.bindImage(ID, 5L)).thenReturn(1);
        service.bindImages(source() + source(), 5L);
        verify(imageMapper).bindImage(ID, 5L);
    }

    @Test void restrictedPostsRejectPublicExternalReferenceAndHtmlImages()
    {
        assertDoesNotThrow(() -> service.validateContent(source(), "MANAGERS"));
        for (String content : List.of("![旧图](/profile/upload/qt/forum/a.png)",
                "![外链](https://example.com/x.png)", "![ref][id]\n[id]: https://example.com/x.png",
                "<img src='https://example.com/x.png'>"))
        {
            assertEquals(400, assertThrows(ServiceException.class, () -> service.validateContent(content, "MANAGERS")).getCode());
            assertDoesNotThrow(() -> service.validateContent(content, "MEMBERS"));
        }
    }

    @Test void pathsAreNotClientControlled()
    {
        QtForumImage image = image();
        assertTrue(service.resolve(image).startsWith(directory.resolve("upload/qt/forum-private")));
        image.setStoragePath("../outside.png");
        assertThrows(ServiceException.class, () -> service.resolve(image));
        assertThrows(ServiceException.class, () -> service.readableImage("../" + ID));
    }

    @Test void uploadUsesValidationAndCleansOnlyItsNewFileOnDatabaseFailure() throws Exception
    {
        byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aCxsAAAAASUVORK5CYII=");
        var upload = new org.springframework.mock.web.MockMultipartFile("file", "diagram.png", "image/png", png);
        when(imageMapper.insertImage(any())).thenReturn(1);
        String url = service.upload(upload);
        assertTrue(url.matches("/qt/forum/images/[a-f0-9]{32}"));
        var capture = org.mockito.ArgumentCaptor.forClass(QtForumImage.class);
        verify(imageMapper).insertImage(capture.capture());
        Path first = service.resolve(capture.getValue());
        assertTrue(java.nio.file.Files.exists(first));
        when(imageMapper.insertImage(any())).thenThrow(new IllegalStateException("DB unavailable"));
        assertThrows(IllegalStateException.class, () -> service.upload(upload));
        try (var files = java.nio.file.Files.list(first.getParent())) { assertEquals(1, files.count()); }
        assertThrows(Exception.class, () -> service.upload(new org.springframework.mock.web.MockMultipartFile("file", "bad.png", "image/png", "<script/>".getBytes())));
    }

    private QtForumImage image()
    {
        QtForumImage image = new QtForumImage(); image.setImageId(ID); image.setOwnerId(8L);
        image.setMediaType("image/png"); image.setStoragePath("upload/qt/forum-private/" + ID + ".png");
        return image;
    }
    private String source() { return "![示意图](/qt/forum/images/" + ID + ")"; }
    private void login(Long id)
    {
        SysUser user = new SysUser(); user.setUserId(id); user.setIsQuantaMember("1");
        LoginUser principal = new LoginUser(id, 100L, user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
