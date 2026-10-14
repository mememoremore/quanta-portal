package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 招新面评 qt_interview_evaluation
 */
public class QtInterviewEvaluation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long evaluationId;
    private Long applicationId;
    private Long roundId;
    private String department;
    private Long evaluatorUserId;
    private String content;
    private String evaluatorName;
    private String evaluatorUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedTime;

    public Long getEvaluationId()
    {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId)
    {
        this.evaluationId = evaluationId;
    }

    public Long getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(Long applicationId)
    {
        this.applicationId = applicationId;
    }

    public Long getRoundId()
    {
        return roundId;
    }

    public void setRoundId(Long roundId)
    {
        this.roundId = roundId;
    }

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
    }

    public Long getEvaluatorUserId()
    {
        return evaluatorUserId;
    }

    public void setEvaluatorUserId(Long evaluatorUserId)
    {
        this.evaluatorUserId = evaluatorUserId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getEvaluatorName()
    {
        return evaluatorName;
    }

    public void setEvaluatorName(String evaluatorName)
    {
        this.evaluatorName = evaluatorName;
    }

    public String getEvaluatorUserName()
    {
        return evaluatorUserName;
    }

    public void setEvaluatorUserName(String evaluatorUserName)
    {
        this.evaluatorUserName = evaluatorUserName;
    }

    public Date getCreatedTime()
    {
        return createdTime != null ? createdTime : getCreateTime();
    }

    public void setCreatedTime(Date createdTime)
    {
        this.createdTime = createdTime;
        setCreateTime(createdTime);
    }

    public Date getUpdatedTime()
    {
        return updatedTime != null ? updatedTime : getUpdateTime();
    }

    public void setUpdatedTime(Date updatedTime)
    {
        this.updatedTime = updatedTime;
        setUpdateTime(updatedTime);
    }
}
