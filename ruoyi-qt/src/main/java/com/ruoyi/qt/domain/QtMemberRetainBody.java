package com.ruoyi.qt.domain;

/**
 * 留任请求
 */
public class QtMemberRetainBody
{
    private Long sourceCohortId;
    private Long targetCohortId;
    private Boolean retain = Boolean.TRUE;

    public Long getSourceCohortId()
    {
        return sourceCohortId;
    }

    public void setSourceCohortId(Long sourceCohortId)
    {
        this.sourceCohortId = sourceCohortId;
    }

    public Long getTargetCohortId()
    {
        return targetCohortId;
    }

    public void setTargetCohortId(Long targetCohortId)
    {
        this.targetCohortId = targetCohortId;
    }

    public Boolean getRetain()
    {
        return retain;
    }

    public void setRetain(Boolean retain)
    {
        this.retain = retain;
    }
}
