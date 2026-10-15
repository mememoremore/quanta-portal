package com.ruoyi.qt.service.impl;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewResult;
import com.ruoyi.qt.domain.QtInterviewRound;
import com.ruoyi.qt.mapper.QtInterviewMapper;
import com.ruoyi.qt.service.IQtInterviewAdminService;
import com.ruoyi.system.service.ISysConfigService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QtInterviewOfferAcceptanceTest
{
    private static final Long APP_ID = 10L;
    private static final Long USER_ID = 88L;
    private static final String FIRST = "BACKEND";
    private static final String SECOND = "PRODUCT";

    @Mock
    private QtInterviewMapper qtInterviewMapper;
    @Mock
    private QtInterviewNotifier interviewNotifier;
    @Mock
    private ISysConfigService configService;
    @Mock
    private IQtInterviewAdminService qtInterviewAdminService;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private QtInterviewServiceImpl service;

    private MockedStatic<DictUtils> dictUtilsMock;
    private QtInterviewApplication application;

    @BeforeEach
    void setUp()
    {
        dictUtilsMock = org.mockito.Mockito.mockStatic(DictUtils.class);
        dictUtilsMock.when(() -> DictUtils.getDictCache(any())).thenReturn(null);
        application = new QtInterviewApplication();
        application.setApplicationId(APP_ID);
        application.setUserId(USER_ID);
        application.setFirstChoice(FIRST);
        application.setSecondChoice(SECOND);
        application.setApplyStatus("PROCESSING");
        application.setRealName("李同学");
        when(qtInterviewMapper.selectApplicationByUserId(USER_ID)).thenReturn(application);
        when(qtInterviewMapper.selectApplicationByIdForUpdate(APP_ID)).thenReturn(application);
        when(qtInterviewMapper.selectRoundByNo(1)).thenReturn(round(1, 101L));
        when(qtInterviewMapper.selectRoundByNo(2)).thenReturn(round(2, 102L));
        login();
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
        dictUtilsMock.close();
    }

    @Test
    void readyOnlyWhenBothChoicesSettled()
    {
        stubResult(101L, FIRST, "PASS");
        stubResult(101L, SECOND, "OUT");
        Map<String, Object> notReady = service.buildOfferAcceptance(USER_ID);
        assertFalse((Boolean) notReady.get("ready"));

        stubResult(102L, FIRST, "PASS");
        Map<String, Object> ready = service.buildOfferAcceptance(USER_ID);
        assertTrue((Boolean) ready.get("ready"));
        assertNull(ready.get("acceptedDepartment"));
    }

    @Test
    void firstRoundOutDoesNotWaitForSecondRound()
    {
        stubResult(101L, FIRST, "OUT");
        stubResult(101L, SECOND, "PASS");
        stubResult(102L, SECOND, "PASS");
        Map<String, Object> ready = service.buildOfferAcceptance(USER_ID);
        assertTrue((Boolean) ready.get("ready"));
    }

    @Test
    void acceptPassDepartmentConvertsOnce()
    {
        stubResult(101L, FIRST, "PASS");
        stubResult(101L, SECOND, "OUT");
        stubResult(102L, FIRST, "PASS");
        Map<String, Object> data = service.acceptOffer(USER_ID, FIRST);
        assertEquals(FIRST, data.get("acceptedDepartment"));
        assertEquals("ACCEPTED", application.getJoinStatus());
        assertEquals(FIRST, application.getOfferedDepartment());
        verify(qtInterviewAdminService).convertToMember(application, String.valueOf(USER_ID));
        verify(tokenService).delLoginUser("tok");
        verify(tokenService).revokeUserSessions(USER_ID);
    }

    @Test
    void acceptSameDepartmentIsIdempotent()
    {
        application.setJoinStatus("ACCEPTED");
        application.setOfferedDepartment(FIRST);
        Map<String, Object> data = service.acceptOffer(USER_ID, FIRST);
        assertEquals(FIRST, data.get("acceptedDepartment"));
        verify(qtInterviewAdminService, never()).convertToMember(any(), anyString());
    }

    @Test
    void cannotSwitchAcceptedDepartment()
    {
        application.setJoinStatus("ACCEPTED");
        application.setOfferedDepartment(FIRST);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.acceptOffer(USER_ID, SECOND));
        assertTrue(ex.getMessage().contains("改选"));
    }

    @Test
    void cannotAcceptFailedDepartment()
    {
        stubResult(101L, FIRST, "PASS");
        stubResult(101L, SECOND, "PASS");
        stubResult(102L, FIRST, "PASS");
        stubResult(102L, SECOND, "OUT");
        ServiceException ex = assertThrows(ServiceException.class, () -> service.acceptOffer(USER_ID, SECOND));
        assertTrue(ex.getMessage().contains("未通过"));
        verify(qtInterviewAdminService, never()).convertToMember(any(), anyString());
    }

    private void stubResult(Long roundId, String department, String status)
    {
        QtInterviewResult result = new QtInterviewResult();
        result.setApplicationId(APP_ID);
        result.setRoundId(roundId);
        result.setDepartment(department);
        result.setResultStatus(status);
        when(qtInterviewMapper.selectResultByAppRoundDept(APP_ID, roundId, department)).thenReturn(result);
    }

    private static QtInterviewRound round(int no, Long id)
    {
        QtInterviewRound round = new QtInterviewRound();
        round.setRoundNo(no);
        round.setRoundId(id);
        return round;
    }

    private void login()
    {
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setUserName("freshman");
        user.setIsQuantaMember("0");
        LoginUser loginUser = new LoginUser(USER_ID, null, user, Set.of());
        loginUser.setToken("tok");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }
}
