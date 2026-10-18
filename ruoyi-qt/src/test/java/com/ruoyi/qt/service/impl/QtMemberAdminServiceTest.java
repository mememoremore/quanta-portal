package com.ruoyi.qt.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.qt.domain.QtCohort;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMemberManageBody;
import com.ruoyi.qt.domain.QtMemberRecord;
import com.ruoyi.qt.mapper.QtCohortMapper;
import com.ruoyi.qt.mapper.QtLabMemberMapper;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtMemberAdminServiceTest
{
    @Mock
    private QtLabMemberMapper qtLabMemberMapper;

    @Mock
    private QtCohortMapper qtCohortMapper;

    @Mock
    private ISysUserService userService;

    @Mock
    private ISysRoleService roleService;

    @Mock
    private ISysConfigService configService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private QtLabMemberServiceImpl service;

    @BeforeEach
    void loginCeo()
    {
        login(1L, "ceo");
    }

    @AfterEach
    void clearLogin()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsTechnicalAdminAndOtherRoles()
    {
        login(2L, "admin");
        ServiceException admin = assertThrows(ServiceException.class, () -> service.managementOptions());
        assertEquals(HttpStatus.FORBIDDEN, admin.getCode());

        login(3L, "qt_manager");
        ServiceException manager = assertThrows(ServiceException.class, () -> service.managementOptions());
        assertEquals(HttpStatus.FORBIDDEN, manager.getCode());
    }

    @Test
    void returnsRealNameWithoutReplacingTheLoginNickname()
    {
        SysUser user = member(7L, "20241001", "张三", role(60L, "qt_member"));
        stubUser(user);
        when(qtCohortMapper.selectRecord(7L, 21L)).thenReturn(record("MEMBER"));
        Map<String, Object> data = service.managedMember(7L, "21");
        assertEquals("张三", data.get("nickName"));
        assertEquals("20241001", data.get("userName"));
        assertEquals("21", data.get("memberCohort"));
        assertEquals(Boolean.TRUE, data.get("canEdit"));
    }

    @Test
    void returnsEmptyCohortForUnassignedMember()
    {
        SysUser user = member(7L, "20241001", "", role(60L, "qt_member"));
        user.setMemberTitle("");
        when(userService.selectUserById(7L)).thenReturn(user);
        when(qtCohortMapper.selectLatestRecordByUser(7L)).thenReturn(null);

        Map<String, Object> data = service.managedMember(7L, "unassigned");

        assertEquals("", data.get("memberCohort"));
        assertEquals("MEMBER", data.get("roleCategory"));
        assertEquals("成员", data.get("memberTitle"));
        assertEquals(Boolean.TRUE, data.get("canEdit"));
    }

    @Test
    void createsMemberWithRealNameAndMemberRoleOnly()
    {
        when(userService.checkUserNameUnique(any())).thenReturn(true);
        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init#1");
        when(roleService.selectRoleAll()).thenReturn(List.of(role(60L, "qt_member"), role(70L, "qt_mgmt")));
        when(qtCohortMapper.selectCohortById(21L)).thenReturn(cohort());
        when(userService.insertUser(any())).thenAnswer(invocation -> {
            SysUser created = invocation.getArgument(0);
            created.setUserId(50L);
            return 1;
        });

        QtMemberManageBody body = body("create");
        body.setRoleCategory("INTERN");
        body.setMemberTitle("实习生");
        body.setNickName("实名");
        body.setUserName("20241001");
        body.setVersion(null);
        Map<String, Object> saved = service.createManagedMember(body, "ceo");

        ArgumentCaptor<SysUser> created = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).insertUser(created.capture());
        assertEquals("20241001", created.getValue().getNickName());
        assertEquals("20241001", created.getValue().getUserName());
        assertEquals("21", created.getValue().getMemberCohort());
        assertEquals("1", created.getValue().getIsQuantaMember());
        assertArrayEquals(new Long[] { 60L }, created.getValue().getRoleIds());
        assertNotEquals("Init#1", created.getValue().getPassword());
        verify(userService).updateRealName(50L, "实名");
        verify(userService, never()).updateUser(any());
        ArgumentCaptor<QtMemberRecord> record = ArgumentCaptor.forClass(QtMemberRecord.class);
        verify(qtCohortMapper).insertRecord(record.capture());
        assertEquals("INTERN", record.getValue().getRoleCategory());
        assertEquals(21L, record.getValue().getCohortId());
        assertEquals(50L, saved.get("userId"));
        assertFalse(saved.containsKey("password"));
    }

    @Test
    void identityUpdatesRealNameAndDepartmentWithoutTouchingNicknameOrRoles()
    {
        SysUser user = member(7L, "20241001", "旧名", role(60L, "qt_member"), role(80L, "qt_manager"));
        user.setMemberTitle("经理");
        user.setUpdateTime(new Date(1_700_000_000_000L));
        stubUser(user);
        when(qtCohortMapper.selectLatestRecordByUser(7L)).thenReturn(record("MANAGER"));
        QtMemberManageBody body = body("identity");
        body.setNickName("新名");
        body.setMemberDepartment("BACKEND");
        body.setRoleCategory("MANAGER");
        body.setMemberTitle("经理");
        body.setVersion("1700000000");

        service.updateManagedMember(7L, body, "ceo");

        verify(userService).updateRealName(7L, "新名");
        ArgumentCaptor<SysUser> placement = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateMemberPlacement(placement.capture());
        assertEquals("BACKEND", placement.getValue().getMemberDepartment());
        assertEquals(null, placement.getValue().getNickName());
        assertEquals(null, placement.getValue().getMemberTitle());
        verify(userService, never()).updateUser(any());
        verify(roleService, never()).deleteAuthUser(any());
        verify(roleService, never()).insertAuthUsers(any(), any());
    }

    @Test
    void accessSwapsManagementRoleAndKeepsUnrelatedRoles()
    {
        SysUser user = member(7L, "20241001", "张三", role(60L, "qt_member"), role(80L, "qt_manager"), role(90L, "qt_custom"));
        user.setMemberTitle("经理");
        user.setUpdateTime(new Date(1_700_000_000_000L));
        stubUser(user);
        when(qtCohortMapper.selectLatestRecordByUser(7L)).thenReturn(record("MANAGER"));
        when(roleService.selectRoleAll()).thenReturn(List.of(role(60L, "qt_member"), role(70L, "qt_mgmt"), role(80L, "qt_manager")));
        QtMemberManageBody body = body("access");
        body.setRoleCategory("MGMT");
        body.setMemberTitle("VP");
        body.setVersion("1700000000");

        service.updateManagedMember(7L, body, "ceo");

        ArgumentCaptor<SysUserRole> removed = ArgumentCaptor.forClass(SysUserRole.class);
        verify(roleService).deleteAuthUser(removed.capture());
        assertEquals(80L, removed.getValue().getRoleId());
        assertEquals(7L, removed.getValue().getUserId());
        verify(roleService).insertAuthUsers(eq(70L), eq(new Long[] { 7L }));
        verify(roleService, never()).insertAuthUsers(eq(60L), any());
        verify(tokenService).revokeUserSessions(7L);
        verify(userService, never()).updateUser(any());
        verify(userService, never()).updateRealName(any(), any());
        ArgumentCaptor<QtMemberRecord> record = ArgumentCaptor.forClass(QtMemberRecord.class);
        verify(qtCohortMapper).updateRecord(record.capture());
        assertEquals("MGMT", record.getValue().getRoleCategory());
    }

    @Test
    void rejectsStaleVersionProtectedSelfAndProtectedCeo()
    {
        SysUser user = member(7L, "20241001", "张三", role(60L, "qt_member"));
        user.setUpdateTime(new Date(1_700_000_000_000L));
        stubUser(user);
        QtMemberManageBody stale = body("identity");
        stale.setNickName("新名");
        stale.setRoleCategory("MEMBER");
        stale.setMemberTitle("成员");
        stale.setVersion("1");
        ServiceException conflict = assertThrows(ServiceException.class, () -> service.updateManagedMember(7L, stale, "ceo"));
        assertEquals(HttpStatus.CONFLICT, conflict.getCode());

        login(7L, "ceo");
        stale.setVersion("1700000000");
        ServiceException self = assertThrows(ServiceException.class, () -> service.updateManagedMember(7L, stale, "ceo"));
        assertEquals(HttpStatus.FORBIDDEN, self.getCode());

        login(1L, "ceo");
        user.setRoles(List.of(role(1L, "ceo"), role(60L, "qt_member")));
        ServiceException protectedCeo = assertThrows(ServiceException.class,
                () -> service.updateManagedMember(7L, stale, "ceo"));
        assertEquals(HttpStatus.FORBIDDEN, protectedCeo.getCode());
        verify(userService, never()).updateRealName(any(), any());
    }

    @Test
    void assignsCohortToUnassignedMemberOnIdentitySave()
    {
        SysUser user = member(7L, "20241001", "", role(60L, "qt_member"));
        user.setMemberTitle("");
        user.setUpdateTime(new Date(1_700_000_000_000L));
        stubUser(user);
        when(qtCohortMapper.selectLatestRecordByUser(7L)).thenReturn(null);
        QtMemberManageBody body = body("identity");
        body.setNickName("新名");
        body.setMemberCohort("21");
        body.setRoleCategory("MEMBER");
        body.setMemberTitle("成员");
        body.setVersion("1700000000");

        service.updateManagedMember(7L, body, "ceo");

        verify(userService).updateRealName(7L, "新名");
        ArgumentCaptor<SysUser> placement = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateMemberPlacement(placement.capture());
        assertEquals("FRONTEND", placement.getValue().getMemberDepartment());
        assertEquals("成员", placement.getValue().getMemberTitle());
        assertEquals("21", placement.getValue().getMemberCohort());
        ArgumentCaptor<QtMemberRecord> record = ArgumentCaptor.forClass(QtMemberRecord.class);
        verify(qtCohortMapper).insertRecord(record.capture());
        assertEquals(21L, record.getValue().getCohortId());
        assertEquals("MEMBER", record.getValue().getRoleCategory());
        verify(qtCohortMapper, never()).updateRecord(any());
    }

    @Test
    void movesExistingRecordWhenCohortChanges()
    {
        SysUser user = member(7L, "20241001", "张三", role(60L, "qt_member"));
        user.setMemberTitle("成员");
        user.setUpdateTime(new Date(1_700_000_000_000L));
        stubUser(user);
        QtMemberRecord current = record("MEMBER");
        current.setCohortId(20L);
        when(qtCohortMapper.selectLatestRecordByUser(7L)).thenReturn(current);
        when(qtCohortMapper.selectRecord(7L, 21L)).thenReturn(null);
        QtMemberManageBody body = body("identity");
        body.setNickName("张三");
        body.setMemberCohort("21");
        body.setRoleCategory("MEMBER");
        body.setMemberTitle("成员");
        body.setVersion("1700000000");

        service.updateManagedMember(7L, body, "ceo");

        ArgumentCaptor<QtMemberRecord> moved = ArgumentCaptor.forClass(QtMemberRecord.class);
        verify(qtCohortMapper).updateRecord(moved.capture());
        assertEquals(21L, moved.getValue().getCohortId());
        verify(qtCohortMapper, never()).insertRecord(any());
        ArgumentCaptor<SysUser> placement = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateMemberPlacement(placement.capture());
        assertEquals("21", placement.getValue().getMemberCohort());
    }

    @Test
    void refusesToMoveIntoACohortThatAlreadyHasTheMember()
    {
        SysUser user = member(7L, "20241001", "张三", role(60L, "qt_member"));
        user.setMemberTitle("成员");
        user.setUpdateTime(new Date(1_700_000_000_000L));
        stubUser(user);
        QtMemberRecord current = record("MEMBER");
        current.setCohortId(20L);
        when(qtCohortMapper.selectLatestRecordByUser(7L)).thenReturn(current);
        when(qtCohortMapper.selectRecord(7L, 21L)).thenReturn(record("MEMBER"));
        QtMemberManageBody body = body("identity");
        body.setNickName("张三");
        body.setMemberCohort("21");
        body.setRoleCategory("MEMBER");
        body.setMemberTitle("成员");
        body.setVersion("1700000000");

        ServiceException error = assertThrows(ServiceException.class, () -> service.updateManagedMember(7L, body, "ceo"));
        assertEquals(HttpStatus.CONFLICT, error.getCode());
        verify(qtCohortMapper, never()).updateRecord(any());
    }

    @Test
    void adminListResolvesCohortTabsThroughRecords()
    {
        when(qtCohortMapper.selectCohortById(21L)).thenReturn(cohort());
        QtLabMember query = new QtLabMember();
        query.setMemberCohort("21");
        service.selectAdminMemberList(query);
        ArgumentCaptor<QtLabMember> resolved = ArgumentCaptor.forClass(QtLabMember.class);
        verify(qtLabMemberMapper).selectAdminMemberList(resolved.capture());
        assertEquals(21L, resolved.getValue().getCohortId());
        assertEquals("第21届", resolved.getValue().getCohortName());
        assertEquals(null, resolved.getValue().getMemberCohort());
        assertEquals(Boolean.FALSE, resolved.getValue().getUnassignedCohort());

        QtLabMember unassigned = new QtLabMember();
        unassigned.setMemberCohort("unassigned");
        service.selectAdminMemberList(unassigned);
        ArgumentCaptor<QtLabMember> flagged = ArgumentCaptor.forClass(QtLabMember.class);
        verify(qtLabMemberMapper, org.mockito.Mockito.times(2)).selectAdminMemberList(flagged.capture());
        assertEquals(null, flagged.getValue().getCohortId());
        assertEquals(Boolean.TRUE, flagged.getValue().getUnassignedCohort());
    }

    @Test
    void keepsPaginationOnTheMemberQueryWhenCohortLookupRunsFirst()
    {
        AtomicReference<Page<?>> pageDuringCohortLookup = new AtomicReference<>();
        AtomicReference<Page<?>> pageDuringMemberQuery = new AtomicReference<>();
        when(qtCohortMapper.selectCohortById(21L)).thenAnswer(invocation -> {
            pageDuringCohortLookup.set(PageHelper.getLocalPage());
            return cohort();
        });
        when(qtLabMemberMapper.selectAdminMemberList(any())).thenAnswer(invocation -> {
            pageDuringMemberQuery.set(PageHelper.getLocalPage());
            return Collections.emptyList();
        });

        PageHelper.startPage(2, 10);
        try
        {
            QtLabMember query = new QtLabMember();
            query.setMemberCohort("21");
            service.selectAdminMemberList(query);
        }
        finally
        {
            PageHelper.clearPage();
        }

        assertEquals(null, pageDuringCohortLookup.get());
        assertEquals(2, pageDuringMemberQuery.get().getPageNum());
        assertEquals(10, pageDuringMemberQuery.get().getPageSize());
    }

    @Test
    void refusesToCreateAManagerDirectly()
    {
        QtMemberManageBody body = body("create");
        body.setRoleCategory("MGMT");
        body.setMemberTitle("VP");
        ServiceException error = assertThrows(ServiceException.class, () -> service.createManagedMember(body, "ceo"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
        assertTrue(error.getMessage().contains("普通塔员"));
        verify(userService, never()).insertUser(any());
    }

    private void stubUser(SysUser user)
    {
        when(qtCohortMapper.selectCohortById(21L)).thenReturn(cohort());
        when(userService.selectUserById(user.getUserId())).thenReturn(user);
    }

    private static QtMemberManageBody body(String mode)
    {
        QtMemberManageBody body = new QtMemberManageBody();
        body.setMode(mode);
        body.setMemberDepartment("FRONTEND");
        body.setMemberCohort("21");
        body.setRoleCategory("MEMBER");
        body.setMemberTitle("成员");
        return body;
    }

    private static SysUser member(Long userId, String userName, String realName, SysRole... roles)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setNickName(userName);
        user.setRealName(realName);
        user.setDelFlag("0");
        user.setMemberDepartment("FRONTEND");
        user.setMemberTitle("成员");
        user.setMemberCohort("21");
        user.setRoles(List.of(roles));
        return user;
    }

    private static QtMemberRecord record(String roleCategory)
    {
        QtMemberRecord record = new QtMemberRecord();
        record.setRecordId(3L);
        record.setUserId(7L);
        record.setCohortId(21L);
        record.setRoleCategory(roleCategory);
        return record;
    }

    private static QtCohort cohort()
    {
        QtCohort cohort = new QtCohort();
        cohort.setCohortId(21L);
        cohort.setCohortName("第21届");
        return cohort;
    }

    private static SysRole role(Long roleId, String roleKey)
    {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setRoleKey(roleKey);
        return role;
    }

    private static void login(Long userId, String roleKey)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<String>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
