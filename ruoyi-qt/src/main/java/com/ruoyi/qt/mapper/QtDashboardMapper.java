package com.ruoyi.qt.mapper;

import org.apache.ibatis.annotations.Param;

public interface QtDashboardMapper
{
    int countActiveMembers();

    int countTodayResumes();

    int countPendingReservations();

    int countPendingPayments();
}
