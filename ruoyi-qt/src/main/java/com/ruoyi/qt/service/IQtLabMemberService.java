package com.ruoyi.qt.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMemberRetainBody;

public interface IQtLabMemberService
{
    List<QtLabMember> selectLabMemberList(QtLabMember query);

    List<QtLabMember> selectAdminMemberList(QtLabMember query);

    Map<String, Object> selectCohorts();

    Map<String, Object> retain(Long userId, QtMemberRetainBody body, String operator);

    void rollover();

    String importLabMembers(InputStream inputStream, boolean updateSupport, String operator);
}
