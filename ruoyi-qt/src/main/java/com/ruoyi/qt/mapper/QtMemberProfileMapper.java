package com.ruoyi.qt.mapper;

import com.ruoyi.qt.domain.QtMemberProfile;
import com.ruoyi.qt.domain.QtPagesPublicProfile;

public interface QtMemberProfileMapper
{
    QtMemberProfile selectByUserId(Long userId);

    QtPagesPublicProfile selectPublicProfileByUserId(Long userId);

    int upsert(QtMemberProfile profile);
}
