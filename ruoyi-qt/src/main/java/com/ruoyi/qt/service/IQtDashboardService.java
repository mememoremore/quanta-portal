package com.ruoyi.qt.service;

import java.util.Map;

/**
 * 控制台统计Service接口
 *
 * @author Quanta
 */
public interface IQtDashboardService
{
    /**
     * 汇总控制台指标（成员数、今日简历、待审预约、待确认付款）。
     *
     * @return 指标集合
     */
    public Map<String, Object> selectStats();
}
