package com.ruoyi.framework.metrics;

import java.math.BigDecimal;

/**
 * 单个 URI（或全局 *）在一个采样窗口内的聚合结果。
 */
public class AccessMetricsRow
{
    public static final String GLOBAL_URI = "*";
    public static final String GLOBAL_METHOD = "*";

    private String uri;
    private String method;
    private long requestCount;
    private long error4xx;
    private long error5xx;
    private int avgCostMs;
    private int maxCostMs;
    private BigDecimal qps;
    private Integer inFlightMax;
    private Integer jvmUsedMb;
    private Integer jvmMaxMb;
    private Integer dbActive;
    private Integer dbMax;
    private long windowMs;

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

    public long getRequestCount()
    {
        return requestCount;
    }

    public void setRequestCount(long requestCount)
    {
        this.requestCount = requestCount;
    }

    public long getError4xx()
    {
        return error4xx;
    }

    public void setError4xx(long error4xx)
    {
        this.error4xx = error4xx;
    }

    public long getError5xx()
    {
        return error5xx;
    }

    public void setError5xx(long error5xx)
    {
        this.error5xx = error5xx;
    }

    public int getAvgCostMs()
    {
        return avgCostMs;
    }

    public void setAvgCostMs(int avgCostMs)
    {
        this.avgCostMs = avgCostMs;
    }

    public int getMaxCostMs()
    {
        return maxCostMs;
    }

    public void setMaxCostMs(int maxCostMs)
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

    public long getWindowMs()
    {
        return windowMs;
    }

    public void setWindowMs(long windowMs)
    {
        this.windowMs = windowMs;
    }
}
