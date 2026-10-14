package com.ruoyi.qt.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Service;
import com.alibaba.druid.pool.DruidDataSource;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.metrics.AccessMetricsCollector;
import com.ruoyi.framework.metrics.AccessMetricsRow;
import com.ruoyi.framework.metrics.AccessMetricsSnapshot;
import com.ruoyi.qt.domain.QtAccessMetric;
import com.ruoyi.qt.mapper.QtAccessMetricMapper;
import com.ruoyi.qt.service.IQtAccessMetricService;

@Service
public class QtAccessMetricServiceImpl implements IQtAccessMetricService
{
    private static final Logger log = LoggerFactory.getLogger(QtAccessMetricServiceImpl.class);

    @Autowired
    private AccessMetricsCollector collector;

    @Autowired
    private QtAccessMetricMapper metricMapper;

    @Value("${qt.metrics.top-uris:20}")
    private int topUris;

    @Value("${qt.metrics.retain-days:30}")
    private int retainDays;

    @Override
    public AccessMetricsSnapshot latest()
    {
        AccessMetricsSnapshot snapshot = collector.peek(topUris);
        enrichRuntime(snapshot.getGlobal());
        return snapshot;
    }

    @Override
    public List<QtAccessMetric> selectGlobalList(Date beginTime, Date endTime)
    {
        Date[] range = defaultRange(beginTime, endTime);
        return metricMapper.selectGlobalList(range[0], range[1]);
    }

    @Override
    public List<QtAccessMetric> selectUriAgg(Date beginTime, Date endTime)
    {
        Date[] range = defaultRange(beginTime, endTime);
        return metricMapper.selectUriAgg(range[0], range[1]);
    }

    @Override
    public void flush()
    {
        AccessMetricsSnapshot snapshot = collector.snapshotAndReset(topUris);
        enrichRuntime(snapshot.getGlobal());
        List<QtAccessMetric> rows = new ArrayList<QtAccessMetric>();
        rows.add(toEntity(snapshot, snapshot.getGlobal()));
        for (AccessMetricsRow uriRow : snapshot.getTopUris())
        {
            rows.add(toEntity(snapshot, uriRow));
        }
        metricMapper.insertBatch(rows);
        AccessMetricsRow global = snapshot.getGlobal();
        log.info("access-metric qps={} avg={} max={} err4={} err5={} inFlightMax={} dbActive={}",
                global.getQps(), global.getAvgCostMs(), global.getMaxCostMs(), global.getError4xx(),
                global.getError5xx(), global.getInFlightMax(), global.getDbActive());
    }

    @Override
    public int cleanExpired()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -Math.max(1, retainDays));
        int deleted = metricMapper.deleteBefore(calendar.getTime());
        if (deleted > 0)
        {
            log.info("access-metric cleaned {} rows older than {} days", deleted, retainDays);
        }
        return deleted;
    }

    private Date[] defaultRange(Date beginTime, Date endTime)
    {
        Date end = endTime != null ? endTime : new Date();
        Date begin = beginTime;
        if (begin == null)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(end);
            calendar.add(Calendar.HOUR_OF_DAY, -24);
            begin = calendar.getTime();
        }
        return new Date[] { begin, end };
    }

    private QtAccessMetric toEntity(AccessMetricsSnapshot snapshot, AccessMetricsRow row)
    {
        QtAccessMetric entity = new QtAccessMetric();
        entity.setSnapshotTime(snapshot.getSnapshotTime());
        entity.setWindowMs((int) Math.min(Integer.MAX_VALUE, snapshot.getWindowMs()));
        entity.setUri(row.getUri());
        entity.setMethod(row.getMethod());
        entity.setRequestCount(toInt(row.getRequestCount()));
        entity.setError4xx(toInt(row.getError4xx()));
        entity.setError5xx(toInt(row.getError5xx()));
        entity.setAvgCostMs(row.getAvgCostMs());
        entity.setMaxCostMs(row.getMaxCostMs());
        entity.setQps(row.getQps());
        entity.setInFlightMax(row.getInFlightMax());
        entity.setJvmUsedMb(row.getJvmUsedMb());
        entity.setJvmMaxMb(row.getJvmMaxMb());
        entity.setDbActive(row.getDbActive());
        entity.setDbMax(row.getDbMax());
        return entity;
    }

    private void enrichRuntime(AccessMetricsRow global)
    {
        Runtime runtime = Runtime.getRuntime();
        global.setJvmUsedMb((int) ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)));
        global.setJvmMaxMb((int) (runtime.maxMemory() / (1024 * 1024)));
        DruidDataSource druid = resolveDruid();
        if (druid != null)
        {
            global.setDbActive(druid.getActiveCount());
            global.setDbMax(druid.getMaxActive());
        }
    }

    private DruidDataSource resolveDruid()
    {
        try
        {
            DataSource dataSource = SpringUtils.getBean("dynamicDataSource");
            if (dataSource instanceof DruidDataSource)
            {
                return (DruidDataSource) dataSource;
            }
            if (dataSource instanceof AbstractRoutingDataSource routing)
            {
                DataSource resolved = routing.getResolvedDefaultDataSource();
                if (resolved instanceof DruidDataSource)
                {
                    return (DruidDataSource) resolved;
                }
            }
        }
        catch (Exception ex)
        {
            log.debug("skip druid stats: {}", ex.getMessage());
        }
        return null;
    }

    private static int toInt(long value)
    {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }
}
