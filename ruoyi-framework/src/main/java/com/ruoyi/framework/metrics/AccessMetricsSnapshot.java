package com.ruoyi.framework.metrics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * One sample window: a global row plus top URIs.
 */
public class AccessMetricsSnapshot
{
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date snapshotTime;
    private long windowStartMs;
    private long windowEndMs;
    private long windowMs;
    private AccessMetricsRow global = new AccessMetricsRow();
    private List<AccessMetricsRow> topUris = new ArrayList<AccessMetricsRow>();

    public Date getSnapshotTime()
    {
        return snapshotTime;
    }

    public void setSnapshotTime(Date snapshotTime)
    {
        this.snapshotTime = snapshotTime;
    }

    public long getWindowStartMs()
    {
        return windowStartMs;
    }

    public void setWindowStartMs(long windowStartMs)
    {
        this.windowStartMs = windowStartMs;
    }

    public long getWindowEndMs()
    {
        return windowEndMs;
    }

    public void setWindowEndMs(long windowEndMs)
    {
        this.windowEndMs = windowEndMs;
    }

    public long getWindowMs()
    {
        return windowMs;
    }

    public void setWindowMs(long windowMs)
    {
        this.windowMs = windowMs;
    }

    public AccessMetricsRow getGlobal()
    {
        return global;
    }

    public void setGlobal(AccessMetricsRow global)
    {
        this.global = global;
    }

    public List<AccessMetricsRow> getTopUris()
    {
        return topUris;
    }

    public void setTopUris(List<AccessMetricsRow> topUris)
    {
        this.topUris = topUris;
    }
}
