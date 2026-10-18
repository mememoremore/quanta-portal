package com.ruoyi.qt.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMemberProfile;
import com.ruoyi.qt.domain.QtMemberProfileUpdateBody;
import com.ruoyi.qt.domain.QtMemberProfileView;
import com.ruoyi.qt.mapper.QtLabMemberMapper;
import com.ruoyi.qt.mapper.QtMemberProfileMapper;
import com.ruoyi.qt.service.IQtMemberProfileService;
import com.ruoyi.qt.util.QtMemberProfileValidator;

@Service
public class QtMemberProfileServiceImpl implements IQtMemberProfileService
{
    @Autowired
    private QtLabMemberMapper labMemberMapper;

    @Autowired
    private QtMemberProfileMapper profileMapper;

    @Override
    public QtMemberProfileView selectOwnProfile()
    {
        Long userId = SecurityUtils.getUserId();
        QtLabMember member = requireVisibleMember(userId);
        return buildView(member, profileMapper.selectByUserId(userId), true);
    }

    @Override
    @Transactional
    public void saveOwnProfile(QtMemberProfileUpdateBody body)
    {
        QtMemberProfileUpdateBody normalized = QtMemberProfileValidator.normalize(body);
        QtMemberProfileValidator.validate(normalized);
        QtMemberProfile row = new QtMemberProfile();
        row.setUserId(SecurityUtils.getUserId());
        row.setCommunityNickname(normalized.getCommunityNickname());
        row.setHeadline(normalized.getHeadline());
        row.setBio(normalized.getBio());
        row.setSkills(JSON.toJSONString(normalized.getSkills()));
        row.setGithubUrl(normalized.getGithubUrl());
        row.setWebsiteUrl(normalized.getWebsiteUrl());
        row.setContactEmail(normalized.getContactEmail());
        row.setShowContactEmail(normalized.getShowContactEmail());
        profileMapper.upsert(row);
    }

    @Override
    public QtMemberProfileView selectPublicProfile(Long userId)
    {
        QtLabMember member = requireVisibleMember(userId);
        return buildView(member, profileMapper.selectByUserId(userId), false);
    }

    @Override
    public QtLabMember requireVisibleMember(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("成员不存在", HttpStatus.NOT_FOUND);
        }
        QtLabMember member = labMemberMapper.selectVisibleQuantaMember(userId);
        if (member == null)
        {
            throw new ServiceException("成员不存在", HttpStatus.NOT_FOUND);
        }
        return member;
    }

    private QtMemberProfileView buildView(QtLabMember member, QtMemberProfile profile, boolean own)
    {
        QtMemberProfileView view = new QtMemberProfileView();
        view.setUserId(member.getUserId());
        view.setNickName(member.getNickName());
        view.setRealName(member.getRealName());
        view.setAvatar(member.getAvatar());
        view.setMemberDepartment(member.getMemberDepartment());
        view.setMemberTitle(member.getMemberTitle());
        view.setMemberCohort(member.getMemberCohort());
        if (own)
        {
            view.setMemberNo(member.getMemberNo());
        }
        if (profile != null)
        {
            view.setCommunityNickname(StringUtils.defaultString(profile.getCommunityNickname()));
            view.setHeadline(StringUtils.defaultString(profile.getHeadline()));
            view.setBio(StringUtils.defaultString(profile.getBio()));
            view.setSkills(parseSkills(profile.getSkills()));
            view.setGithubUrl(StringUtils.defaultString(profile.getGithubUrl()));
            view.setWebsiteUrl(StringUtils.defaultString(profile.getWebsiteUrl()));
            boolean showEmail = Boolean.TRUE.equals(profile.getShowContactEmail());
            view.setShowContactEmail(showEmail);
            if (own)
            {
                view.setContactEmail(StringUtils.defaultString(profile.getContactEmail()));
            }
            else
            {
                view.setContactEmail(showEmail ? StringUtils.defaultString(profile.getContactEmail()) : "");
            }
        }
        else
        {
            view.setCommunityNickname("");
            view.setHeadline("");
            view.setBio("");
            view.setSkills(Collections.emptyList());
            view.setGithubUrl("");
            view.setWebsiteUrl("");
            view.setContactEmail("");
            view.setShowContactEmail(false);
        }
        return view;
    }

    private List<String> parseSkills(String raw)
    {
        if (StringUtils.isEmpty(raw))
        {
            return Collections.emptyList();
        }
        try
        {
            List<String> skills = JSON.parseArray(raw, String.class);
            return skills == null ? Collections.emptyList() : skills;
        }
        catch (Exception ex)
        {
            return Collections.emptyList();
        }
    }
}
