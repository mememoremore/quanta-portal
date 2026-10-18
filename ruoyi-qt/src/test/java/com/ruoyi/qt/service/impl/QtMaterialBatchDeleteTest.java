package com.ruoyi.qt.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.domain.QtMaterialFileCleanup;
import com.ruoyi.qt.domain.vo.QtMaterialBatchDeleteResult;
import com.ruoyi.qt.mapper.QtMaterialFileCleanupMapper;
import com.ruoyi.qt.mapper.QtMaterialMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtMaterialBatchDeleteTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private QtMaterialMapper qtMaterialMapper;

    @Mock
    private QtMaterialFileCleanupMapper qtMaterialFileCleanupMapper;

    @Mock
    private com.ruoyi.qt.service.IQtMaterialFolderService qtMaterialFolderService;

    @InjectMocks
    private QtMaterialServiceImpl service;

    @TempDir
    Path profile;

    @BeforeEach
    void profile()
    {
        new RuoYiConfig().setProfile(profile.toString());
        lenient().when(qtMaterialFileCleanupMapper.selectPending()).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ceoWithRemovePermissionDeletesAndKeepsAlreadyMissingIds() throws Exception
    {
        login(8L, "ceo", Set.of("qt:material:remove"));
        when(qtMaterialMapper.selectMaterialsByIdsForUpdate(List.of(101L, 102L)))
                .thenReturn(List.of(stored(101L)));
        when(qtMaterialMapper.deleteMaterialsByIds(List.of(101L))).thenReturn(1);

        QtMaterialBatchDeleteResult result = service.batchDelete(json("{\"materialIds\":[101,102]}"));

        assertEquals(List.of(101L), result.getDeletedIds());
        assertEquals(List.of(102L), result.getAlreadyDeletedIds());
        verify(qtMaterialFileCleanupMapper).insertCleanup(any(QtMaterialFileCleanup.class));
    }

    @Test
    void ceoWithAllPermissionCanDelete() throws Exception
    {
        login(8L, "ceo", Set.of("*:*:*"));
        when(qtMaterialMapper.selectMaterialsByIdsForUpdate(List.of(7L))).thenReturn(List.of(stored(7L)));
        when(qtMaterialMapper.deleteMaterialsByIds(List.of(7L))).thenReturn(1);
        QtMaterialBatchDeleteResult result = service.batchDelete(json("{\"materialIds\":[7]}"));
        assertEquals(List.of(7L), result.getDeletedIds());
        assertTrue(result.getAlreadyDeletedIds().isEmpty());
    }

    @Test
    void repeatedRequestReportsAlreadyDeletedWithoutDeletingAgain() throws Exception
    {
        login(8L, "ceo", Set.of("qt:material:remove"));
        when(qtMaterialMapper.selectMaterialsByIdsForUpdate(List.of(101L, 102L))).thenReturn(Collections.emptyList());
        QtMaterialBatchDeleteResult result = service.batchDelete(json("{\"materialIds\":[101,102]}"));
        assertTrue(result.getDeletedIds().isEmpty());
        assertEquals(List.of(101L, 102L), result.getAlreadyDeletedIds());
        verify(qtMaterialMapper, never()).deleteMaterialsByIds(anyList());
        verify(qtMaterialFileCleanupMapper, never()).insertCleanup(any());
    }

    @Test
    void adminSuperPermissionWithoutCeoRoleIsRejectedBeforeLookup() throws Exception
    {
        login(1L, "admin", Set.of("*:*:*"));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.batchDelete(json("{\"materialIds\":[101]}")));
        assertEquals(HttpStatus.FORBIDDEN, error.getCode());
        assertEquals("仅 CEO 可执行批量删除", error.getMessage());
        verifyNoInteractions(qtMaterialMapper);
    }

    @Test
    void managementRolesCannotBatchDelete() throws Exception
    {
        for (String role : List.of("qt_mgmt", "qt_manager", "qt_member"))
        {
            login(9L, role, Set.of("qt:material:remove", "*:*:*"));
            ServiceException error = assertThrows(ServiceException.class,
                    () -> service.batchDelete(json("{\"materialIds\":[101]}")));
            assertEquals(HttpStatus.FORBIDDEN, error.getCode());
        }
        verifyNoInteractions(qtMaterialMapper);
    }

    @Test
    void ceoWithoutRemovePermissionIsRejected() throws Exception
    {
        login(8L, "ceo", Set.of("qt:material:add"));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.batchDelete(json("{\"materialIds\":[101]}")));
        assertEquals(HttpStatus.FORBIDDEN, error.getCode());
        verifyNoInteractions(qtMaterialMapper);
    }

    @Test
    void illegalPayloadsDoNotTouchStoredFiles() throws Exception
    {
        login(8L, "ceo", Set.of("qt:material:remove"));
        for (String payload : List.of(
                "{\"materialIds\":[]}",
                "{\"materialIds\":[0]}",
                "{\"materialIds\":[-1]}",
                "{\"materialIds\":[1,1]}",
                "{\"materialIds\":[1.5]}",
                "{\"materialIds\":[\"101\"]}",
                "{\"materialIds\":[101],\"category\":\"FRONTEND\"}",
                "{}"))
        {
            ServiceException error = assertThrows(ServiceException.class, () -> service.batchDelete(json(payload)));
            assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        }
        StringBuilder tooMany = new StringBuilder("{\"materialIds\":[");
        for (int i = 1; i <= 21; i++)
        {
            if (i > 1)
            {
                tooMany.append(',');
            }
            tooMany.append(i);
        }
        tooMany.append("]}");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ServiceException.class, () -> service.batchDelete(json(tooMany.toString()))).getCode());
        verifyNoInteractions(qtMaterialMapper);
    }

    @Test
    void untrustedPathRejectsTheWholeBatchBeforeDelete() throws Exception
    {
        login(8L, "ceo", Set.of("qt:material:remove"));
        QtMaterial outside = stored(101L);
        outside.setFilePath("/profile/upload/avatar/secret.pdf");
        when(qtMaterialMapper.selectMaterialsByIdsForUpdate(List.of(101L, 102L)))
                .thenReturn(List.of(outside, stored(102L)));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.batchDelete(json("{\"materialIds\":[101,102]}")));
        assertEquals(HttpStatus.CONFLICT, error.getCode());
        verify(qtMaterialMapper, never()).deleteMaterialsByIds(anyList());
        verify(qtMaterialFileCleanupMapper, never()).insertCleanup(any());
    }

    @Test
    void deleteCountMismatchRollsBackCleanup() throws Exception
    {
        login(8L, "ceo", Set.of("qt:material:remove"));
        when(qtMaterialMapper.selectMaterialsByIdsForUpdate(List.of(101L))).thenReturn(List.of(stored(101L)));
        when(qtMaterialMapper.deleteMaterialsByIds(List.of(101L))).thenReturn(0);
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.batchDelete(json("{\"materialIds\":[101]}")));
        assertEquals(HttpStatus.CONFLICT, error.getCode());
        verify(qtMaterialFileCleanupMapper, never()).insertCleanup(any());
    }

    @Test
    void purgeDeletesOnlyFilesInsideTheMaterialRoot() throws Exception
    {
        Path file = profile.resolve(Path.of("upload", "qt", "materials", "note.pdf"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, "pdf");
        QtMaterialFileCleanup task = new QtMaterialFileCleanup();
        task.setCleanupId(4L);
        task.setFilePath("/profile/upload/qt/materials/note.pdf");
        when(qtMaterialFileCleanupMapper.selectPending()).thenReturn(List.of(task));

        service.purgePendingFileCleanups();

        assertFalse(Files.exists(file));
        verify(qtMaterialFileCleanupMapper).markResult(4L, "DONE", null);
    }

    @Test
    void purgeMarksUntrustedPathFailedWithoutLeavingTheRoot() throws Exception
    {
        Path outside = profile.resolve("secret.txt");
        Files.writeString(outside, "keep");
        QtMaterialFileCleanup task = new QtMaterialFileCleanup();
        task.setCleanupId(5L);
        task.setFilePath("/profile/secret.txt");
        when(qtMaterialFileCleanupMapper.selectPending()).thenReturn(List.of(task));

        service.purgePendingFileCleanups();

        assertTrue(Files.exists(outside));
        verify(qtMaterialFileCleanupMapper).markResult(5L, "FAILED", "文件清理失败");
    }

    @Test
    void cleanupRecordUsesTheStoredPath() throws Exception
    {
        login(8L, "ceo", Set.of("qt:material:remove"));
        when(qtMaterialMapper.selectMaterialsByIdsForUpdate(List.of(101L))).thenReturn(List.of(stored(101L)));
        when(qtMaterialMapper.deleteMaterialsByIds(List.of(101L))).thenReturn(1);
        service.batchDelete(json("{\"materialIds\":[101]}"));
        ArgumentCaptor<QtMaterialFileCleanup> captor = ArgumentCaptor.forClass(QtMaterialFileCleanup.class);
        verify(qtMaterialFileCleanupMapper).insertCleanup(captor.capture());
        assertEquals("/profile/upload/qt/materials/a.pdf", captor.getValue().getFilePath());
        assertEquals(101L, captor.getValue().getMaterialId());
    }

    private JsonNode json(String payload) throws Exception
    {
        return objectMapper.readTree(payload);
    }

    private QtMaterial stored(Long id)
    {
        QtMaterial material = new QtMaterial();
        material.setMaterialId(id);
        material.setFilePath("/profile/upload/qt/materials/a.pdf");
        return material;
    }

    private void login(Long userId, String roleKey, Set<String> permissions)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("operator");
        user.setIsQuantaMember("1");
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
