package com.ruoyi.qt.pages;

import java.util.Collections;
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
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtLabMemberMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtPagesAccessTest
{
    @Mock
    private QtPageSiteMapper siteMapper;

    @Mock
    private QtLabMemberMapper labMemberMapper;

    @InjectMocks
    private QtPagesAccess access;

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void notLoggedInReturns401()
    {
        ServiceException exception = assertThrows(ServiceException.class, access::requireCurrentMemberId);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getCode());
    }

    @Test
    void nonMemberReturns403()
    {
        login(7L, "0", List.of());
        ServiceException exception = assertThrows(ServiceException.class, access::requireCurrentMemberId);
        assertEquals(HttpStatus.FORBIDDEN, exception.getCode());
    }

    @Test
    void disabledOrRemovedMemberReturns403()
    {
        login(7L, "1", List.of());
        when(labMemberMapper.selectVisibleQuantaMember(7L)).thenReturn(null);
        ServiceException exception = assertThrows(ServiceException.class, access::requireCurrentMemberId);
        assertEquals(HttpStatus.FORBIDDEN, exception.getCode());
    }

    @Test
    void validMemberPassesAndReturnsId()
    {
        login(7L, "1", List.of());
        when(labMemberMapper.selectVisibleQuantaMember(7L)).thenReturn(new QtLabMember());
        assertEquals(7L, access.requireCurrentMemberId());
    }

    @Test
    void missingOrForeignSiteReturns404EvenForAdmin()
    {
        login(7L, "1", List.of(role("admin"), role("ceo")));
        when(labMemberMapper.selectVisibleQuantaMember(7L)).thenReturn(new QtLabMember());
        when(siteMapper.selectById(99L)).thenReturn(null);

        ServiceException missing = assertThrows(ServiceException.class, () -> access.requireOwnedSite(99L));
        assertEquals(HttpStatus.NOT_FOUND, missing.getCode());

        QtPageSite foreign = new QtPageSite();
        foreign.setSiteId(99L);
        foreign.setUserId(8L);
        when(siteMapper.selectById(99L)).thenReturn(foreign);

        ServiceException foreignSite = assertThrows(ServiceException.class, () -> access.requireOwnedSite(99L));
        assertEquals(HttpStatus.NOT_FOUND, foreignSite.getCode());
    }

    @Test
    void ownedSiteIsReturned()
    {
        login(7L, "1", List.of());
        when(labMemberMapper.selectVisibleQuantaMember(7L)).thenReturn(new QtLabMember());
        QtPageSite own = new QtPageSite();
        own.setSiteId(9L);
        own.setUserId(7L);
        when(siteMapper.selectById(9L)).thenReturn(own);
        assertEquals(own, access.requireOwnedSite(9L));
    }

    private static SysRole role(String roleKey)
    {
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        return role;
    }

    private void login(Long userId, String isQuantaMember, List<SysRole> roles)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setIsQuantaMember(isQuantaMember);
        user.setRoles(roles);
        LoginUser loginUser = new LoginUser(userId, 100L, user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
