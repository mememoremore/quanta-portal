package com.ruoyi.qt;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.controller.QtInterviewAdminController;
import com.ruoyi.qt.controller.QtInterviewController;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewEvaluation;
import com.ruoyi.qt.domain.QtInterviewOfferBody;
import com.ruoyi.qt.domain.QtInterviewResult;
import com.ruoyi.qt.domain.QtInterviewRound;
import com.ruoyi.qt.mapper.QtInterviewMapper;
import com.ruoyi.qt.service.impl.QtInterviewAdminServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtInterviewEvaluationThreadTest
{
    private static final String EVAL_A = "eval-A";
    private static final String EVAL_B = "eval-B";
    private static final String EVAL_OLD = "eval-old";
    private static final String EVAL_HACK = "eval-hack";
    private static final String NICK = "nick-display";
    private static final String CONTENT = "eval-content";
    private static final String FORBIDDEN = "\u65e0\u6743\u4fee\u6539\u4ed6\u4eba\u9762\u8bc4";

    @Mock
    private QtInterviewMapper qtInterviewMapper;

    @InjectMocks
    private QtInterviewAdminServiceImpl service;

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void twoEvaluatorsWriteIndependentRows()
    {
        login(8L, "1", "tower_a", Set.of());
        when(qtInterviewMapper.selectApplicationById(10L)).thenReturn(application());
        when(qtInterviewMapper.selectRoundByNo(1)).thenReturn(roundOne());
        when(qtInterviewMapper.selectEvaluationByUnique(eq(10L), eq(101L), eq("BACKEND"), eq(8L))).thenReturn(null);
        when(qtInterviewMapper.selectEvaluationByUnique(eq(10L), eq(101L), eq("BACKEND"), eq(9L))).thenReturn(null);
        when(qtInterviewMapper.insertEvaluation(any())).thenAnswer(invocation -> {
            QtInterviewEvaluation row = invocation.getArgument(0);
            row.setEvaluationId(row.getEvaluatorUserId());
            return 1;
        });
        when(qtInterviewMapper.selectEvaluationById(8L)).thenReturn(stored(8L, "tower_a", "nickA", EVAL_A));
        when(qtInterviewMapper.selectEvaluationById(9L)).thenReturn(stored(9L, "tower_b", "nickB", EVAL_B));

        QtInterviewEvaluation first = body("BACKEND", 1L, EVAL_A);
        first.setEvaluatorUserId(99L);
        service.saveEvaluation(first, 8L, "tower_a");
        service.saveEvaluation(body("BACKEND", 1L, EVAL_B), 9L, "tower_b");

        ArgumentCaptor<QtInterviewEvaluation> captor = ArgumentCaptor.forClass(QtInterviewEvaluation.class);
        verify(qtInterviewMapper, times(2)).insertEvaluation(captor.capture());
        verify(qtInterviewMapper, never()).updateEvaluation(any());
        List<QtInterviewEvaluation> inserted = captor.getAllValues();
        assertEquals(8L, inserted.get(0).getEvaluatorUserId());
        assertEquals(9L, inserted.get(1).getEvaluatorUserId());
        assertEquals(101L, inserted.get(0).getRoundId());
        assertEquals(101L, inserted.get(1).getRoundId());
    }

    @Test
    void cannotUpdateOthersEvaluation()
    {
        login(8L, "1", "tower_a", Set.of());
        QtInterviewEvaluation old = stored(1L, "tower_b", "nickB", EVAL_OLD);
        old.setEvaluatorUserId(9L);
        when(qtInterviewMapper.selectEvaluationById(1L)).thenReturn(old);

        QtInterviewEvaluation patch = new QtInterviewEvaluation();
        patch.setEvaluationId(1L);
        patch.setContent(EVAL_HACK);
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updateEvaluation(patch, 8L, "tower_a"));
        assertEquals(FORBIDDEN, error.getMessage());
        verify(qtInterviewMapper, never()).updateEvaluation(any());
    }

    @Test
    void listReturnsLoginUserNameNotNickAndDoesNotForceOwnDepartment()
    {
        login(1L, "1", "ceo", Set.of("*:*:*"), "ceo");
        when(qtInterviewMapper.selectApplicationById(10L)).thenReturn(application());
        when(qtInterviewMapper.selectRoundByNo(1)).thenReturn(roundOne());
        QtInterviewEvaluation row = stored(1L, "tower_a", NICK, CONTENT);
        when(qtInterviewMapper.selectEvaluationList(any())).thenReturn(List.of(row));

        QtInterviewEvaluation query = new QtInterviewEvaluation();
        query.setApplicationId(10L);
        query.setRoundId(1L);
        query.setDepartment("PRODUCT");
        List<QtInterviewEvaluation> list = service.selectEvaluations(query);

        ArgumentCaptor<QtInterviewEvaluation> captor = ArgumentCaptor.forClass(QtInterviewEvaluation.class);
        verify(qtInterviewMapper).selectEvaluationList(captor.capture());
        assertEquals("PRODUCT", captor.getValue().getDepartment());
        assertEquals(101L, captor.getValue().getRoundId());
        assertEquals("tower_a", list.get(0).getEvaluatorUserName());
        assertEquals(NICK, list.get(0).getEvaluatorName());
        assertFalse(list.get(0).getEvaluatorUserName().equals(list.get(0).getEvaluatorName()));
    }

    @Test
    void evaluationSqlUsesUserNameAndChronologicalOrder() throws Exception
    {
        byte[] bytes = getClass().getClassLoader().getResourceAsStream("mapper/QtInterviewMapper.xml").readAllBytes();
        String xml = new String(bytes, StandardCharsets.UTF_8);
        int listStart = xml.indexOf("id=\"selectEvaluationList\"");
        int listEnd = xml.indexOf("</select>", listStart);
        String listSql = xml.substring(listStart, listEnd);
        assertTrue(listSql.contains("u.user_name as evaluator_user_name"));
        assertTrue(listSql.contains("u.nick_name as evaluator_name"));
        assertTrue(listSql.contains("order by e.create_time asc"));
        assertFalse(listSql.contains("order by e.create_time desc"));
    }

    @Test
    void evaluationApisRequireListOrEvaluatePermission() throws Exception
    {
        Method get = QtInterviewAdminController.class.getMethod("evaluations", QtInterviewEvaluation.class);
        Method post = QtInterviewAdminController.class.getMethod("addEvaluation", QtInterviewEvaluation.class);
        Method put = QtInterviewAdminController.class.getMethod("editEvaluation", Long.class, QtInterviewEvaluation.class);
        assertTrue(get.getAnnotation(PreAuthorize.class).value().contains("qt:interview:admin:list"));
        assertTrue(post.getAnnotation(PreAuthorize.class).value().contains("qt:interview:admin:evaluate"));
        assertTrue(put.getAnnotation(PreAuthorize.class).value().contains("qt:interview:admin:evaluate"));

        Method result = QtInterviewController.class.getMethod("saveResult", QtInterviewResult.class);
        PreAuthorize resultAuth = result.getAnnotation(PreAuthorize.class);
        assertNotNull(resultAuth);
        assertTrue(resultAuth.value().contains("qt:interview:admin:evaluate"));
    }

    @Test
    void freshmanCannotWriteEvaluation()
    {
        login(8L, "0", "freshman", Set.of());
        QtInterviewAdminController controller = new QtInterviewAdminController();
        assertThrows(ServiceException.class, () -> controller.addEvaluation(body("BACKEND", 1L, EVAL_A)));
        assertThrows(ServiceException.class, () -> controller.evaluations(new QtInterviewEvaluation()));
        assertThrows(ServiceException.class, () -> controller.editEvaluation(1L, body("BACKEND", 1L, EVAL_A)));
    }

    @Test
    void regularMemberCannotSaveInterviewResult()
    {
        login(8L, "1", "tower_a", Set.of("qt:interview:admin:evaluate"));
        QtInterviewController controller = new QtInterviewController();
        assertThrows(ServiceException.class, () -> controller.saveResult(new QtInterviewResult()));
    }

    @Test
    void managerCannotSaveInterviewResult()
    {
        login(8L, "1", "manager_a", Set.of("qt:interview:admin:evaluate"), "qt_manager");
        QtInterviewController controller = new QtInterviewController();
        ServiceException error = assertThrows(ServiceException.class, () -> controller.saveResult(new QtInterviewResult()));
        assertTrue(error.getMessage().contains("\u4ec5\u7ba1\u7406\u5c42"));
    }

    @Test
    void managementCanPassInterviewResultAuthButNotOtherDepartment()
    {
        login(8L, "1", "qt_admin_backend", Set.of("qt:interview:admin:evaluate"), "qt_mgmt");
        QtInterviewController controller = new QtInterviewController();
        QtInterviewResult missingUser = new QtInterviewResult();
        Object code = controller.saveResult(missingUser).get(com.ruoyi.common.core.domain.AjaxResult.CODE_TAG);
        assertEquals(com.ruoyi.common.constant.HttpStatus.ERROR, code);

        QtInterviewResult otherDept = new QtInterviewResult();
        otherDept.setUserId(88L);
        otherDept.setRoundId(1L);
        otherDept.setDepartment("PRODUCT");
        otherDept.setResultStatus("PASS");
        ServiceException error = assertThrows(ServiceException.class, () -> controller.saveResult(otherDept));
        assertTrue(error.getMessage().contains("\u672c\u90e8\u95e8"));
    }

    @Test
    void adminListApisRequireInterviewListPermission() throws Exception
    {
        Method apps = QtInterviewAdminController.class.getMethod("applications",
                QtInterviewApplication.class, Long.class, String.class, String.class);
        Method detail = QtInterviewAdminController.class.getMethod("application", Long.class);
        Method results = QtInterviewAdminController.class.getMethod("results", Long.class);
        Method stats = QtInterviewAdminController.class.getMethod("statistics");
        for (Method m : new Method[]{apps, detail, results, stats})
        {
            PreAuthorize auth = m.getAnnotation(PreAuthorize.class);
            assertNotNull(auth, m.getName() + " should require qt:interview:admin:list");
            assertTrue(auth.value().contains("qt:interview:admin:list"), m.getName());
        }

        Method offers = QtInterviewAdminController.class.getMethod("offers", QtInterviewOfferBody.class);
        PreAuthorize offersAuth = offers.getAnnotation(PreAuthorize.class);
        assertNotNull(offersAuth);
        assertTrue(offersAuth.value().contains("qt:interview:admin:offer"));
    }

    private void login(Long userId, String memberFlag, String userName, Set<String> permissions)
    {
        login(userId, memberFlag, userName, permissions, "qt_member");
    }

    private void login(Long userId, String memberFlag, String userName, Set<String> permissions, String roleKey)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setIsQuantaMember(memberFlag);
        user.setMemberDepartment("BACKEND");
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>(permissions));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private QtInterviewApplication application()
    {
        QtInterviewApplication application = new QtInterviewApplication();
        application.setApplicationId(10L);
        application.setFirstChoice("BACKEND");
        application.setSecondChoice("PRODUCT");
        application.setApplyStatus("PROCESSING");
        return application;
    }

    private QtInterviewRound roundOne()
    {
        QtInterviewRound round = new QtInterviewRound();
        round.setRoundId(101L);
        round.setRoundNo(1);
        return round;
    }

    private QtInterviewEvaluation body(String department, Long roundId, String content)
    {
        QtInterviewEvaluation evaluation = new QtInterviewEvaluation();
        evaluation.setApplicationId(10L);
        evaluation.setDepartment(department);
        evaluation.setRoundId(roundId);
        evaluation.setContent(content);
        return evaluation;
    }

    private QtInterviewEvaluation stored(Long evaluationId, String userName, String nick, String content)
    {
        QtInterviewEvaluation evaluation = new QtInterviewEvaluation();
        evaluation.setEvaluationId(evaluationId);
        evaluation.setApplicationId(10L);
        evaluation.setRoundId(101L);
        evaluation.setDepartment("BACKEND");
        evaluation.setEvaluatorUserId(evaluationId);
        evaluation.setEvaluatorUserName(userName);
        evaluation.setEvaluatorName(nick);
        evaluation.setContent(content);
        return evaluation;
    }
}
