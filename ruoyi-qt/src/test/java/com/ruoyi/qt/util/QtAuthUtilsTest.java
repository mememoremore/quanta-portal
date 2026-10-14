package com.ruoyi.qt.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.controller.QtBookBorrowController;
import com.ruoyi.qt.controller.QtBookController;
import com.ruoyi.qt.controller.QtClothingItemController;
import com.ruoyi.qt.controller.QtClothingOrderController;
import com.ruoyi.qt.controller.QtLabMemberController;
import com.ruoyi.qt.controller.QtMaterialController;
import com.ruoyi.qt.controller.QtPaymentConfigController;
import com.ruoyi.qt.controller.QtWorkstationController;
import com.ruoyi.qt.controller.QtWorkstationReservationController;
import com.ruoyi.qt.domain.QtBook;
import com.ruoyi.qt.domain.QtBookBorrow;
import com.ruoyi.qt.domain.QtClothingItem;
import com.ruoyi.qt.domain.QtClothingOrder;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.domain.QtPaymentConfig;
import com.ruoyi.qt.domain.QtWorkstation;
import com.ruoyi.qt.domain.QtWorkstationReservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QtAuthUtilsTest
{
    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void memberCannotReadOthersRecord()
    {
        login(8L, Set.of("qt:other:list"));
        assertThrows(ServiceException.class,
                () -> QtAuthUtils.assertOwnerOrAdmin(9L, QtAuthUtils.PERM_BORROW_LIST));
    }

    @Test
    void memberCanReadOwnRecord()
    {
        login(8L, Set.of("qt:other:list"));
        assertDoesNotThrow(() -> QtAuthUtils.assertOwnerOrAdmin(8L, QtAuthUtils.PERM_BORROW_LIST));
    }

    @Test
    void listAdminCanReadOthers()
    {
        login(8L, Set.of(QtAuthUtils.PERM_BORROW_LIST));
        assertDoesNotThrow(() -> QtAuthUtils.assertOwnerOrAdmin(9L, QtAuthUtils.PERM_BORROW_LIST));
    }

    @Test
    void restrictToSelfSetsUserIdForMember()
    {
        login(8L, "1", Set.of("qt:other:list"));
        Long[] holder = new Long[1];
        QtAuthUtils.restrictToSelfIfNoAdmin(QtAuthUtils.PERM_BORROW_LIST, id -> holder[0] = id);
        org.junit.jupiter.api.Assertions.assertEquals(8L, holder[0]);
    }

    @Test
    void restrictToSelfAlwaysUsesCurrentUserEvenWithListPermission()
    {
        login(8L, "1", Set.of(QtAuthUtils.PERM_BORROW_LIST));
        Long[] holder = new Long[1];
        QtAuthUtils.restrictToSelf(id -> holder[0] = id);
        org.junit.jupiter.api.Assertions.assertEquals(8L, holder[0]);
    }

    @Test
    void backofficeRolesCanReadAllActivitySignups()
    {
        for (String roleKey : List.of("admin", "ceo", "qt_mgmt", "qt_manager"))
        {
            login(8L, "1", Set.of(), roleKey);
            org.junit.jupiter.api.Assertions.assertTrue(QtAuthUtils.isBackofficeUser(), roleKey);
        }
    }

    @Test
    void freshmanIsNotBackofficeUser()
    {
        login(8L, "0", Set.of(), "freshman");
        org.junit.jupiter.api.Assertions.assertFalse(QtAuthUtils.isBackofficeUser());
    }

    @Test
    void requireQuantaMemberRejectsFreshman()
    {
        login(8L, "0", Set.of());
        ServiceException error = assertThrows(ServiceException.class, QtAuthUtils::requireQuantaMember);
        assertEquals("仅塔员可访问该功能", error.getMessage());
    }

    @Test
    void requireQuantaMemberAllowsMemberAndMemberAdmin()
    {
        login(8L, "1", Set.of());
        assertDoesNotThrow(QtAuthUtils::requireQuantaMember);
        login(1L, "1", Set.of("*:*:*"));
        assertDoesNotThrow(QtAuthUtils::requireQuantaMember);
    }

    @Test
    void offerPermissionDoesNotUnscopeRecruitmentDepartment()
    {
        loginDepartment(8L, "BACKEND", Set.of(QtAuthUtils.PERM_INTERVIEW_OFFER), "qt_mgmt");
        org.junit.jupiter.api.Assertions.assertEquals("BACKEND", QtAuthUtils.scopedDepartment());
        loginDepartment(1L, "PRODUCT", Set.of(QtAuthUtils.PERM_INTERVIEW_OFFER), "ceo");
        org.junit.jupiter.api.Assertions.assertNull(QtAuthUtils.scopedDepartment());
    }

    @Test
    void memberPortalControllerEntriesRejectFreshmanBeforeQuerying()
    {
        login(8L, "0", Set.of());
        assertThrows(ServiceException.class, () -> new QtLabMemberController().list(new QtLabMember()));
        assertThrows(ServiceException.class, () -> new QtMaterialController().list(new QtMaterial()));
        assertThrows(ServiceException.class, () -> new QtBookController().list(new QtBook()));
        assertThrows(ServiceException.class, () -> new QtBookBorrowController().list(new QtBookBorrow()));
        assertThrows(ServiceException.class, () -> new QtWorkstationController().list(new QtWorkstation()));
        assertThrows(ServiceException.class,
                () -> new QtWorkstationReservationController().list(new QtWorkstationReservation()));
        assertThrows(ServiceException.class, () -> new QtClothingItemController().list(new QtClothingItem()));
        assertThrows(ServiceException.class, () -> new QtClothingOrderController().list(new QtClothingOrder()));
        assertThrows(ServiceException.class, () -> new QtPaymentConfigController().list(new QtPaymentConfig()));
    }

    @Test
    void memberPortalDetailAndCreateEntriesRejectFreshmanBeforeUsingServices()
    {
        login(8L, "0", Set.of());
        assertThrows(ServiceException.class, () -> new QtMaterialController().getInfo(1L));
        assertThrows(ServiceException.class, () -> new QtMaterialController().download(1L, null));
        assertThrows(ServiceException.class, () -> new QtBookController().getInfo(1L));
        assertThrows(ServiceException.class, () -> new QtBookBorrowController().detailList(new QtBookBorrow()));
        assertThrows(ServiceException.class, () -> new QtBookBorrowController().myDetailList(new QtBookBorrow()));
        assertThrows(ServiceException.class, () -> new QtBookBorrowController().getInfo(1L));
        assertThrows(ServiceException.class, () -> new QtBookBorrowController().add(new QtBookBorrow()));
        assertThrows(ServiceException.class, () -> new QtWorkstationController().getInfo(1L));
        assertThrows(ServiceException.class,
                () -> new QtWorkstationReservationController().detailList(new QtWorkstationReservation()));
        assertThrows(ServiceException.class,
                () -> new QtWorkstationReservationController().myDetailList(new QtWorkstationReservation()));
        assertThrows(ServiceException.class, () -> new QtWorkstationReservationController().getInfo(1L));
        assertThrows(ServiceException.class,
                () -> new QtWorkstationReservationController().add(new QtWorkstationReservation()));
        assertThrows(ServiceException.class, () -> new QtClothingItemController().getInfo(1L));
        assertThrows(ServiceException.class, () -> new QtClothingOrderController().detailList(new QtClothingOrder()));
        assertThrows(ServiceException.class, () -> new QtClothingOrderController().myDetailList(new QtClothingOrder()));
        assertThrows(ServiceException.class, () -> new QtClothingOrderController().getInfo(1L));
        assertThrows(ServiceException.class, () -> new QtClothingOrderController().add(new QtClothingOrder()));
        assertThrows(ServiceException.class, () -> new QtPaymentConfigController().getInfo(1L));
    }

    private void login(Long userId, Set<String> permissions)
    {
        login(userId, "1", permissions);
    }

    private void login(Long userId, String memberFlag, Set<String> permissions)
    {
        login(userId, memberFlag, permissions, "qt_member");
    }

    private void login(Long userId, String memberFlag, Set<String> permissions, String roleKey)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setIsQuantaMember(memberFlag);
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>(permissions));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void loginDepartment(Long userId, String department, Set<String> permissions, String roleKey)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setIsQuantaMember("1");
        user.setMemberDepartment(department);
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>(permissions));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
