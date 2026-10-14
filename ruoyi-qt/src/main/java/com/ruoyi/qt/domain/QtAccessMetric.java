package com.ruoyi.qt.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 访问指标快照 qt_access_metric
 */
public class QtAccessMetric
{
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date snapshotTime;

    private Integer windowMs;

    private String uri;

    private String method;

    private Integer requestCount;

    private Integer error4xx;

    private Integer error5xx;

    private Integer avgCostMs;

    private Integer maxCostMs;

    private BigDecimal qps;

    private Integer inFlightMax;

    private Integer jvmUsedMb;

    private Integer jvmMaxMb;

    private Integer dbActive;

    private Integer dbMax;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Date getSnapshotTime()
    {
        return snapshotTime;
    }

    public void setSnapshotTime(Date snapshotTime)
    {
        this.snapshotTime = snapshotTime;
    }

    public Integer getWindowMs()
    {
        return windowMs;
    }

    public void setWindowMs(Integer windowMs)
    {
        this.windowMs = windowMs;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public Integer getRequestCount()
    {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount)
    {
        this.requestCount = requestCount;
    }

    public Integer getError4xx()
    {
        return error4xx;
    }

    public void setError4xx(Integer error4xx)
    {
        this.error4xx = error4xx;
    }

    public Integer getError5xx()
    {
        return error5xx;
    }

    public void setError5xx(Integer error5xx)
    {
        this.error5xx = error5xx;
    }

    public Integer getAvgCostMs()
    {
        return avgCostMs;
    }

    public void setAvgCostMs(Integer avgCostMs)
    {
        this.avgCostMs = avgCostMs;
    }

    public Integer getMaxCostMs()
    {
        return maxCostMs;
    }

    public void setMaxCostMs(Integer maxCostMs)
    {
        this.maxCostMs = maxCostMs;
    }

    public BigDecimal getQps()
    {
        return qps;
    }

    public void setQps(BigDecimal qps)
    {
        this.qps = qps;
    }

    public Integer getInFlightMax()
    {
        return inFlightMax;
    }

    public void setInFlightMax(Integer inFlightMax)
    {
        this.inFlightMax = inFlightMax;
    }

    public Integer getJvmUsedMb()
    {
        return jvmUsedMb;
    }

    public void setJvmUsedMb(Integer jvmUsedMb)
    {
        this.jvmUsedMb = jvmUsedMb;
    }

    public Integer getJvmMaxMb()
    {
        return jvmMaxMb;
    }

    public void setJvmMaxMb(Integer jvmMaxMb)
    {
        this.jvmMaxMb = jvmMaxMb;
    }

    public Integer getDbActive()
    {
        return dbActive;
    }

    public void setDbActive(Integer dbActive)
    {
        this.dbActive = dbActive;
    }

    public Integer getDbMax()
    {
        return dbMax;
    }

    public void setDbMax(Integer dbMax)
    {
        this.dbMax = dbMax;
    }

    public Date getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(Date beginTime)
    {
        this.beginTime = beginTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }
}
