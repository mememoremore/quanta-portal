package com.ruoyi.qt.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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
import com.ruoyi.qt.mapper.QtMaterialMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学习资料四分类上传规则：四类白名单、MEMBER 可见性、部门绑定。
 */
@ExtendWith(MockitoExtension.class)
class QtMaterialServiceRulesTest
{
    @Mock
    private QtMaterialMapper qtMaterialMapper;

    @InjectMocks
    private QtMaterialServiceImpl service;

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void insertRejectsMissingAndUnknownCategory()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        ServiceException missing = assertThrows(ServiceException.class,
                () -> service.insertMaterial(material(null, "MEMBER")));
        assertEquals(HttpStatus.BAD_REQUEST, missing.getCode());
        ServiceException unknown = assertThrows(ServiceException.class,
                () -> service.insertMaterial(material("UI", "MEMBER")));
        assertEquals(HttpStatus.BAD_REQUEST, unknown.getCode());
        verify(qtMaterialMapper, never()).insertMaterial(any(QtMaterial.class));
    }

    @Test
    void insertRejectsNonMemberVisibility()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.insertMaterial(material("FRONTEND", "DEPT")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        verify(qtMaterialMapper, never()).insertMaterial(any(QtMaterial.class));
    }

    @Test
    void insertRejectsCrossDepartmentCategory()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.insertMaterial(material("BACKEND", "MEMBER")));
        assertEquals(HttpStatus.FORBIDDEN, error.getCode());
        verify(qtMaterialMapper, never()).insertMaterial(any(QtMaterial.class));
    }

    @Test
    void insertRejectsAccountWithoutDepartment()
    {
        login(8L, null, Set.of("qt:material:add"));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.insertMaterial(material("FRONTEND", "MEMBER")));
        assertEquals(HttpStatus.FORBIDDEN, error.getCode());
    }

    @Test
    void insertAcceptsOwnDepartmentAndForcesMemberVisibility()
    {
        login(8L, "FRONTEND", Set.of("qt:material:add"));
        QtMaterial material = material("FRONTEND", "");
        when(qtMaterialMapper.insertMaterial(material)).thenReturn(1);
        assertEquals(1, service.insertMaterial(material));
        assertEquals("MEMBER", material.getVisibility());
    }

    @Test
    void adminCanUploadAnyStandardCategory()
    {
        login(1L, "FRONTEND", Set.of("*:*:*"));
        for (String category : List.of("FRONTEND", "BACKEND", "PRODUCT", "DESIGN"))
        {
            QtMaterial material = material(category, "MEMBER");
            when(qtMaterialMapper.insertMaterial(material)).thenReturn(1);
            assertEquals(1, service.insertMaterial(material));
        }
    }

    @Test
    void adminWithoutDepartmentCanStillUpload()
    {
        login(1L, null, Set.of("*:*:*"));
        QtMaterial material = material("BACKEND", "MEMBER");
        when(qtMaterialMapper.insertMaterial(material)).thenReturn(1);
        assertEquals(1, service.insertMaterial(material));
        assertEquals("MEMBER", material.getVisibility());
    }

    @Test
    void chineseDepartmentIsNormalizedBeforeCategoryCheck()
    {
        login(8L, "\u524d\u7aef", Set.of("qt:material:add"));
        QtMaterial own = material("FRONTEND", "MEMBER");
        when(qtMaterialMapper.insertMaterial(own)).thenReturn(1);
        assertEquals(1, service.insertMaterial(own));

        ServiceException cross = assertThrows(ServiceException.class,
                () -> service.insertMaterial(material("BACKEND", "MEMBER")));
        assertEquals(HttpStatus.FORBIDDEN, cross.getCode());
    }

    @Test
    void standardScopePassesThroughToListQuery()
    {
        login(8L, "FRONTEND", Set.of());
        QtMaterial query = new QtMaterial();
        query.setCategoryScope("STANDARD");
        when(qtMaterialMapper.selectMaterialList(query)).thenReturn(Collections.emptyList());
        service.selectMaterialList(query);
        assertEquals("STANDARD", query.getCategoryScope());
        verify(qtMaterialMapper).selectMaterialList(query);
    }

    private QtMaterial material(String category, String visibility)
    {
        QtMaterial material = new QtMaterial();
        material.setFileName("a.pdf");
        material.setCategory(category);
        material.setVisibility(visibility);
        return material;
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
