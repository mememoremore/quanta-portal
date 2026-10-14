package com.ruoyi.qt.service.impl;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.qt.mapper.QtDashboardMapper;
import com.ruoyi.qt.service.IQtDashboardService;

/**
 * 控制台统计Service业务层处理
 * <p>
 * 单次请求要跑 4 个 COUNT 聚合，是后台首页最贵的读；这里整体缓存 60 秒，
 * 指标本身允许一分钟的展示延迟。
 *
 * @author Quanta
 */
@Service
public class QtDashboardServiceImpl implements IQtDashboardService
{
    @Autowired
    private QtDashboardMapper qtDashboardMapper;

    @Override
    @Cacheable(cacheNames = CacheConstants.CACHE_QT_DASHBOARD_STATS, key = "'all'")
    public Map<String, Object> selectStats()
    {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("members", qtDashboardMapper.countActiveMembers());
        data.put("resumesToday", qtDashboardMapper.countTodayResumes());
        data.put("pendingReservations", qtDashboardMapper.countPendingReservations());
        data.put("pendingPayments", qtDashboardMapper.countPendingPayments());
        return data;
    }
}
