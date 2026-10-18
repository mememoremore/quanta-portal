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
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMemberProfile;
import com.ruoyi.qt.domain.QtMemberProfileUpdateBody;
import com.ruoyi.qt.domain.QtMemberProfileView;
import com.ruoyi.qt.mapper.QtLabMemberMapper;
import com.ruoyi.qt.mapper.QtMemberProfileMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class QtMemberProfileServiceTest
{
    @Mock
    private QtLabMemberMapper labMemberMapper;

    @Mock
    private QtMemberProfileMapper profileMapper;

    @InjectMocks
    private QtMemberProfileServiceImpl service;

    @AfterEach
    void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicProfileHidesContactEmailWhenNotShared()
    {
        login(8L);
        QtLabMember member = visibleMember(12L);
        QtMemberProfile profile = new QtMemberProfile();
        profile.setUserId(12L);
        profile.setContactEmail("hidden@example.com");
        profile.setShowContactEmail(false);
        when(labMemberMapper.selectVisibleQuantaMember(12L)).thenReturn(member);
        when(profileMapper.selectByUserId(12L)).thenReturn(profile);
        QtMemberProfileView view = service.selectPublicProfile(12L);
        assertEquals("", view.getContactEmail());
        assertEquals(false, view.getShowContactEmail());
    }

    @Test
    void saveOwnProfileUpsertsEditableFieldsOnly()
    {
        login(8L);
        QtMemberProfileUpdateBody body = new QtMemberProfileUpdateBody();
        body.setHeadline("一句话");
        body.setBio("简介");
        body.setSkills(List.of("Vue"));
        body.setGithubUrl("https://github.com/example");
        body.setCommunityNickname("  橘猫  ");
        service.saveOwnProfile(body);
        ArgumentCaptor<QtMemberProfile> saved = ArgumentCaptor.forClass(QtMemberProfile.class);
        verify(profileMapper).upsert(saved.capture());
        assertEquals("橘猫", saved.getValue().getCommunityNickname());
    }

    @Test
    void blankNicknameClearsStoredValueAndPublicViewKeepsRealName()
    {
        login(8L);
        QtMemberProfileUpdateBody body = new QtMemberProfileUpdateBody();
        body.setCommunityNickname("   ");
        service.saveOwnProfile(body);
        ArgumentCaptor<QtMemberProfile> saved = ArgumentCaptor.forClass(QtMemberProfile.class);
        verify(profileMapper).upsert(saved.capture());
        assertEquals("", saved.getValue().getCommunityNickname());

        QtLabMember member = visibleMember(12L);
        member.setRealName("张三");
        member.setNickName("20251003142");
        QtMemberProfile profile = new QtMemberProfile();
        profile.setCommunityNickname("橘猫");
        when(labMemberMapper.selectVisibleQuantaMember(12L)).thenReturn(member);
        when(profileMapper.selectByUserId(12L)).thenReturn(profile);
        QtMemberProfileView view = service.selectPublicProfile(12L);
        assertEquals("张三", view.getRealName());
        assertEquals("20251003142", view.getNickName());
        assertEquals("橘猫", view.getCommunityNickname());
    }

    @Test
    void missingMemberReturns404()
    {
        login(8L);
        when(labMemberMapper.selectVisibleQuantaMember(99L)).thenReturn(null);
        ServiceException error = assertThrows(ServiceException.class, () -> service.selectPublicProfile(99L));
        assertEquals(HttpStatus.NOT_FOUND, error.getCode());
    }

    @Test
    void emptyProfileStillReturnsIdentityFields()
    {
        login(8L);
        QtLabMember member = visibleMember(8L);
        when(labMemberMapper.selectVisibleQuantaMember(8L)).thenReturn(member);
        when(profileMapper.selectByUserId(8L)).thenReturn(null);
        QtMemberProfileView view = service.selectOwnProfile();
        assertEquals("QT0008", view.getMemberNo());
        assertTrue(view.getSkills().isEmpty());
    }

    private QtLabMember visibleMember(Long userId)
    {
        QtLabMember member = new QtLabMember();
        member.setUserId(userId);
        member.setNickName("塔员");
        member.setMemberNo("QT0008");
        member.setMemberDepartment("FRONTEND");
        member.setMemberTitle("实习生");
        member.setMemberCohort("第21届");
        return member;
    }

    private void login(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setIsQuantaMember("1");
        SysRole role = new SysRole();
        role.setRoleKey("qt_member");
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>(Set.of()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
