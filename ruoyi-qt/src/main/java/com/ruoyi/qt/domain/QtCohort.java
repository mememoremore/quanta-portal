package com.ruoyi.qt.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 实验室届次
 */
public class QtCohort extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long cohortId;
    private String cohortName;
    private Integer cohortYear;
    private String isCurrent;
    private String status;

    public Long getCohortId()
    {
        return cohortId;
    }

    public void setCohortId(Long cohortId)
    {
        this.cohortId = cohortId;
    }

    public String getCohortName()
    {
        return cohortName;
    }

    public void setCohortName(String cohortName)
    {
        this.cohortName = cohortName;
    }

    public Integer getCohortYear()
    {
        return cohortYear;
    }

    public void setCohortYear(Integer cohortYear)
    {
        this.cohortYear = cohortYear;
    }

    public String getIsCurrent()
    {
        return isCurrent;
    }

    public void setIsCurrent(String isCurrent)
    {
        this.isCurrent = isCurrent;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
