package com.ruoyi.qt.service;

import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMemberProfileUpdateBody;
import com.ruoyi.qt.domain.QtMemberProfileView;

public interface IQtMemberProfileService
{
    QtMemberProfileView selectOwnProfile();

    void saveOwnProfile(QtMemberProfileUpdateBody body);

    QtMemberProfileView selectPublicProfile(Long userId);

    QtLabMember requireVisibleMember(Long userId);
}
