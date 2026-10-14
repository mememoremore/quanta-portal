package com.ruoyi.qt;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.qt.domain.QtCohort;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewOfferBody;
import com.ruoyi.qt.domain.QtInterviewResult;
import com.ruoyi.qt.domain.QtInterviewRound;
import com.ruoyi.qt.domain.QtMemberRecord;
import com.ruoyi.qt.mapper.QtCohortMapper;
import com.ruoyi.qt.service.impl.QtInterviewNotifier;
import com.ruoyi.qt.mapper.QtInterviewMapper;
import com.ruoyi.qt.service.impl.QtInterviewAdminServiceImpl;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ??? offer() PASS ???????????????????OUT ????????
 */
@ExtendWith(MockitoExtension.class)
class QtInterviewOfferMemberTest
{
    private static final Long APP_ID = 10L;
    private static final Long USER_ID = 88L;
    private static final Long COHORT_ID = 7L;
    private static final Long QT_MEMBER_ROLE_ID = 6L;
    private static final String DEPT = "BACKEND";

    @Mock
    private QtInterviewMapper qtInterviewMapper;

    @Mock
    private QtInterviewNotifier interviewNotifier;

    @Mock
    private ISysUserService userService;

    @Mock
    private QtCohortMapper qtCohortMapper;

    @Mock
    private ISysRoleService roleService;

    @InjectMocks
    private QtInterviewAdminServiceImpl service;

    private MockedStatic<DictUtils> dictUtilsMock;

    @BeforeEach
    void setUp()
    {
        dictUtilsMock = org.mockito.Mockito.mockStatic(DictUtils.class);
        dictUtilsMock.when(() -> DictUtils.getDictCache(any())).thenReturn(null);
    }

    @AfterEach
    void clear()
    {
        if (dictUtilsMock != null)
        {
            dictUtilsMock.close();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void offerPassConvertsFreshmanToMember()
    {
        loginAsCeo();
        stubRoundTwoReady(DEPT);
        when(userService.selectUserById(USER_ID)).thenReturn(freshmanUser());
        when(roleService.selectRoleAll()).thenReturn(List.of(qtMemberRole()));
        when(qtCohortMapper.selectCurrentCohort()).thenReturn(currentCohort());
        when(qtCohortMapper.selectRecord(eq(USER_ID), eq(COHORT_ID))).thenReturn(null);

        QtInterviewOfferBody body = new QtInterviewOfferBody();
        body.setApplicationId(APP_ID);
        body.setDecision("PASS");
        body.setDepartment(DEPT);

        service.offer(body, "ceo_op");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService, times(1)).updateUserProfile(userCaptor.capture());
        SysUser updated = userCaptor.getValue();
        assertEquals("1", updated.getIsQuantaMember());
        assertEquals(DEPT, updated.getMemberDepartment());
        verify(userService, never()).updateUser(any());
        verify(qtInterviewMapper, times(1)).insertUserRoleIfAbsent(eq(USER_ID), eq(QT_MEMBER_ROLE_ID));
        ArgumentCaptor<QtMemberRecord> recordCaptor = ArgumentCaptor.forClass(QtMemberRecord.class);
        verify(qtCohortMapper, times(1)).insertRecord(recordCaptor.capture());
        QtMemberRecord record = recordCaptor.getValue();
        assertEquals(USER_ID, record.getUserId());
        assertEquals(COHORT_ID, record.getCohortId());
        assertEquals("MEMBER", record.getRoleCategory());
        assertEquals("ACTIVE", record.getMemberStatus());
        assertEquals("0", record.getRetainFlag());
        assertNotNull(record.getJoinTime());
        verify(interviewNotifier, never()).notifyApplicant(anyLong(), any(), any());
    }

    @Test
    void offerPassWithExistingRecordReactivatesAsMember()
    {
        loginAsCeo();
        stubRoundTwoReady(DEPT);
        when(userService.selectUserById(USER_ID)).thenReturn(freshmanUser());
        when(roleService.selectRoleAll()).thenReturn(List.of(qtMemberRole()));
        when(qtCohortMapper.selectCurrentCohort()).thenReturn(currentCohort());
        QtMemberRecord existing = new QtMemberRecord();
        existing.setRecordId(99L);
        existing.setUserId(USER_ID);
        existing.setCohortId(COHORT_ID);
        existing.setRoleCategory("FRESHMAN");
        existing.setMemberStatus("INACTIVE");
        when(qtCohortMapper.selectRecord(eq(USER_ID), eq(COHORT_ID))).thenReturn(existing);

        QtInterviewOfferBody body = new QtInterviewOfferBody();
        body.setApplicationId(APP_ID);
        body.setDecision("PASS");
        body.setDepartment(DEPT);

        service.offer(body, "ceo_op");

        verify(qtCohortMapper, never()).insertRecord(any());
        ArgumentCaptor<QtMemberRecord> recordCaptor = ArgumentCaptor.forClass(QtMemberRecord.class);
        verify(qtCohortMapper, times(1)).updateRecord(recordCaptor.capture());
        QtMemberRecord updated = recordCaptor.getValue();
        assertEquals("MEMBER", updated.getRoleCategory());
        assertEquals("ACTIVE", updated.getMemberStatus());
    }

    @Test
    void offerOutDoesNotConvertToMember()
    {
        loginAsCeo();
        stubRoundTwoReady(DEPT);

        QtInterviewOfferBody body = new QtInterviewOfferBody();
        body.setApplicationId(APP_ID);
        body.setDecision("OUT");
        body.setDepartment(DEPT);

        service.offer(body, "ceo_op");

        verify(userService, never()).updateUser(any());
        verify(userService, never()).updateUserProfile(any());
        verify(qtInterviewMapper, never()).insertUserRoleIfAbsent(anyLong(), anyLong());
        verify(qtCohortMapper, never()).insertRecord(any());
        verify(qtCohortMapper, never()).updateRecord(any());
    }

    @Test
    void offerPassWithoutCurrentCohortStillUpdatesUserAndRole()
    {
        loginAsCeo();
        stubRoundTwoReady(DEPT);
        when(userService.selectUserById(USER_ID)).thenReturn(freshmanUser());
        when(roleService.selectRoleAll()).thenReturn(List.of(qtMemberRole()));
        when(qtCohortMapper.selectCurrentCohort()).thenReturn(null);

        QtInterviewOfferBody body = new QtInterviewOfferBody();
        body.setApplicationId(APP_ID);
        body.setDecision("PASS");
        body.setDepartment(DEPT);

        service.offer(body, "ceo_op");

        verify(userService, times(1)).updateUserProfile(any());
        verify(userService, never()).updateUser(any());
        verify(qtInterviewMapper, times(1)).insertUserRoleIfAbsent(eq(USER_ID), eq(QT_MEMBER_ROLE_ID));
        verify(qtCohortMapper, never()).insertRecord(any());
    }

    @Test
    void offerPassMissingUserThrows()
    {
        loginAsCeo();
        stubRoundTwoReady(DEPT);
        when(userService.selectUserById(USER_ID)).thenReturn(null);

        QtInterviewOfferBody body = new QtInterviewOfferBody();
        body.setApplicationId(APP_ID);
        body.setDecision("PASS");
        body.setDepartment(DEPT);

        ServiceException error = assertThrows(ServiceException.class, () -> service.offer(body, "ceo_op"));
        assertTrue(error.getMessage().contains("\u65e0\u6cd5\u8f6c\u4e3a\u5854\u5458"));
        verify(userService, never()).updateUser(any());
        verify(userService, never()).updateUserProfile(any());
    }

    private void loginAsCeo()
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("ceo");
        user.setIsQuantaMember("1");
        user.setMemberDepartment(null);
        SysRole role = new SysRole();
        role.setRoleKey("ceo");
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(1L, 100L, user, new java.util.HashSet<>(Collections.emptyList()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private QtInterviewApplication application(String applyStatus)
    {
        QtInterviewApplication application = new QtInterviewApplication();
        application.setApplicationId(APP_ID);
        application.setUserId(USER_ID);
        application.setFirstChoice(DEPT);
        application.setSecondChoice("PRODUCT");
        application.setApplyStatus(applyStatus);
        return application;
    }

    private void stubRoundTwoReady(String department)
    {
        when(qtInterviewMapper.selectApplicationById(APP_ID)).thenReturn(application("PROCESSING"));
        when(qtInterviewMapper.selectRoundByNo(1)).thenReturn(roundOne());
        when(qtInterviewMapper.selectRoundByNo(2)).thenReturn(roundTwo());
        QtInterviewResult round1 = new QtInterviewResult();
        round1.setResultStatus("PASS");
        when(qtInterviewMapper.selectResultByAppRoundDept(eq(APP_ID), eq(101L), eq(department))).thenReturn(round1);
        QtInterviewResult round2 = new QtInterviewResult();
        round2.setResultId(200L);
        round2.setResultStatus("PENDING");
        round2.setScore(new BigDecimal("88"));
        when(qtInterviewMapper.selectResultByAppRoundDept(eq(APP_ID), eq(102L), eq(department))).thenReturn(round2);
    }

    private QtInterviewRound roundOne()
    {
        QtInterviewRound round = new QtInterviewRound();
        round.setRoundId(101L);
        round.setRoundNo(1);
        return round;
    }

    private QtInterviewRound roundTwo()
    {
        QtInterviewRound round = new QtInterviewRound();
        round.setRoundId(102L);
        round.setRoundNo(2);
        return round;
    }

    private SysUser freshmanUser()
    {
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setUserName("freshman_88");
        user.setIsQuantaMember("0");
        user.setMemberDepartment(null);
        return user;
    }

    private SysRole qtMemberRole()
    {
        SysRole role = new SysRole();
        role.setRoleId(QT_MEMBER_ROLE_ID);
        role.setRoleKey("qt_member");
        return role;
    }

    private QtCohort currentCohort()
    {
        QtCohort cohort = new QtCohort();
        cohort.setCohortId(COHORT_ID);
        cohort.setCohortName("2026");
        cohort.setIsCurrent("1");
        return cohort;
    }
}
