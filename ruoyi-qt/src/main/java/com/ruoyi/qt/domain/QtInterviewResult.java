package com.ruoyi.qt.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 面试每轮结果对象 qt_interview_result
 */
public class QtInterviewResult extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long resultId;
    private Long applicationId;
    private Long userId;
    private Long roundId;
    private Integer roundNo;
    private String roundName;
    private String department;
    private String resultStatus;
    private BigDecimal score;
    private String feedback;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date interviewTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishedTime;

    public Long getResultId()
    {
        return resultId;
    }

    public void setResultId(Long resultId)
    {
        this.resultId = resultId;
    }

    public Long getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(Long applicationId)
    {
        this.applicationId = applicationId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getRoundId()
    {
        return roundId;
    }

    public void setRoundId(Long roundId)
    {
        this.roundId = roundId;
    }

    public Integer getRoundNo()
    {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo)
    {
        this.roundNo = roundNo;
    }

    public String getRoundName()
    {
        return roundName;
    }

    public void setRoundName(String roundName)
    {
        this.roundName = roundName;
    }

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
    }

    public String getResultStatus()
    {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus)
    {
        this.resultStatus = resultStatus;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public void setScore(BigDecimal score)
    {
        this.score = score;
    }

    public String getFeedback()
    {
        return feedback;
    }

    public void setFeedback(String feedback)
    {
        this.feedback = feedback;
    }

    public Date getInterviewTime()
    {
        return interviewTime;
    }

    public void setInterviewTime(Date interviewTime)
    {
        this.interviewTime = interviewTime;
    }

    public Date getPublishedTime()
    {
        return publishedTime;
    }

    public void setPublishedTime(Date publishedTime)
    {
        this.publishedTime = publishedTime;
    }
}
