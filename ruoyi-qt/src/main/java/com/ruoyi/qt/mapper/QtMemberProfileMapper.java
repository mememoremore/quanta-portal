package com.ruoyi.qt.mapper;

import com.ruoyi.qt.domain.QtMemberProfile;

public interface QtMemberProfileMapper
{
    QtMemberProfile selectByUserId(Long userId);

    int upsert(QtMemberProfile profile);
}
