package com.ruoyi.qt.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.domain.QtMaterialFolder;
import com.ruoyi.qt.mapper.QtMaterialFolderMapper;
import com.ruoyi.qt.service.IQtMaterialService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtMaterialFolderServiceRulesTest
{
    @Mock
    private QtMaterialFolderMapper qtMaterialFolderMapper;

    @Mock
    private IQtMaterialService qtMaterialService;

    @InjectMocks
    private QtMaterialFolderServiceImpl service;

    @BeforeEach
    void stubUploadPolicy()
    {
        lenient().doAnswer(invocation -> null).when(qtMaterialService).validateUploadPolicy(any(QtMaterial.class));
    }

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsDepthBeyondFive()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        QtMaterialFolder parent = folder(9L, 8L, "FRONTEND", "L5", 5);
        when(qtMaterialFolderMapper.selectFolderById(9L)).thenReturn(parent);
        QtMaterialFolder request = new QtMaterialFolder();
        request.setCategory("FRONTEND");
        request.setParentId(9L);
        request.setFolderName("too-deep");
        ServiceException error = assertThrows(ServiceException.class, () -> service.createFolder(request));
        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        verify(qtMaterialFolderMapper, never()).insertFolder(any());
    }

    @Test
    void createRejectsDuplicateSiblingName()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        when(qtMaterialFolderMapper.selectFolderByParentAndName(0L, "FRONTEND", "培训"))
                .thenReturn(folder(2L, 0L, "FRONTEND", "培训", 1));
        QtMaterialFolder request = new QtMaterialFolder();
        request.setCategory("FRONTEND");
        request.setParentId(0L);
        request.setFolderName("培训");
        ServiceException error = assertThrows(ServiceException.class, () -> service.createFolder(request));
        assertEquals(HttpStatus.CONFLICT, error.getCode());
    }

    @Test
    void deleteRejectsNonEmptyFolder()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        when(qtMaterialFolderMapper.selectFolderById(3L)).thenReturn(folder(3L, 0L, "FRONTEND", "培训", 1));
        when(qtMaterialFolderMapper.countChildFolders(3L)).thenReturn(0);
        when(qtMaterialFolderMapper.countMaterialsInFolder(3L)).thenReturn(2);
        assertThrows(ServiceException.class, () -> service.deleteFolder(3L));
        verify(qtMaterialFolderMapper, never()).deleteFolderById(3L);
    }

    @Test
    void moveRejectsDescendantTarget()
    {
        login(1L, "FRONTEND", Set.of("*:*:*"));
        QtMaterialFolder moving = folder(5L, 0L, "FRONTEND", "A", 1);
        QtMaterialFolder child = folder(6L, 5L, "FRONTEND", "B", 2);
        when(qtMaterialFolderMapper.selectFolderById(5L)).thenReturn(moving);
        when(qtMaterialFolderMapper.selectFolderById(6L)).thenReturn(child);
        QtMaterialFolder patch = new QtMaterialFolder();
        patch.setParentId(6L);
        assertThrows(ServiceException.class, () -> service.updateFolder(5L, patch));
    }

    @Test
    void validateUploadRejectsCrossCategoryFolder()
    {
        when(qtMaterialFolderMapper.selectFolderById(7L)).thenReturn(folder(7L, 0L, "BACKEND", "服务端", 1));
        QtMaterial material = new QtMaterial();
        material.setCategory("FRONTEND");
        material.setFolderId(7L);
        assertThrows(ServiceException.class, () -> service.validateMaterialFolderTarget(material));
    }

    @Test
    void createAcceptsRootFolder()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        when(qtMaterialFolderMapper.selectFolderByParentAndName(0L, "FRONTEND", "2026")).thenReturn(null);
        when(qtMaterialFolderMapper.insertFolder(any())).thenReturn(1);
        QtMaterialFolder request = new QtMaterialFolder();
        request.setCategory("FRONTEND");
        request.setParentId(0L);
        request.setFolderName("2026");
        QtMaterialFolder created = service.createFolder(request);
        assertEquals(1, created.getDepth());
        assertEquals("2026", created.getFolderName());
    }

    private QtMaterialFolder folder(Long id, long parentId, String category, String name, int depth)
    {
        QtMaterialFolder folder = new QtMaterialFolder();
        folder.setFolderId(id);
        folder.setParentId(parentId);
        folder.setCategory(category);
        folder.setFolderName(name);
        folder.setDepth(depth);
        return folder;
    }

    private void login(Long userId, String department, Set<String> permissions)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setIsQuantaMember("1");
        user.setMemberDepartment(department);
        SysRole role = new SysRole();
        role.setRoleKey("qt_member");
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>(permissions));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
