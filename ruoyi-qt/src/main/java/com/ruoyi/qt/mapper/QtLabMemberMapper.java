package com.ruoyi.qt.mapper;

import java.util.List;
import com.ruoyi.qt.domain.QtLabMember;

public interface QtLabMemberMapper
{
    List<QtLabMember> selectLabMemberList(QtLabMember query);

    List<QtLabMember> selectAdminMemberList(QtLabMember query);
}
