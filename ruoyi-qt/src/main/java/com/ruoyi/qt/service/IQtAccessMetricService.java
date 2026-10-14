package com.ruoyi.qt.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.framework.metrics.AccessMetricsSnapshot;
import com.ruoyi.qt.domain.QtAccessMetric;

public interface IQtAccessMetricService
{
    AccessMetricsSnapshot latest();

    List<QtAccessMetric> selectGlobalList(Date beginTime, Date endTime);

    List<QtAccessMetric> selectUriAgg(Date beginTime, Date endTime);

    void flush();

    int cleanExpired();
}
