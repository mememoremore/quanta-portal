package com.ruoyi.qt.domain;

/**
 * 二面录用/淘汰请求体
 */
public class QtInterviewOfferBody
{
    private Long applicationId;
    private Integer volunteerNo;
    private String department;
    private Long roundId;
    /** PASS / OUT */
    private String decision;
    private String notice;

    public Long getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(Long applicationId)
    {
        this.applicationId = applicationId;
    }

    public Integer getVolunteerNo()
    {
        return volunteerNo;
    }

    public void setVolunteerNo(Integer volunteerNo)
    {
        this.volunteerNo = volunteerNo;
    }

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
    }

    public Long getRoundId()
    {
        return roundId;
    }

    public void setRoundId(Long roundId)
    {
        this.roundId = roundId;
    }

    public String getDecision()
    {
        return decision;
    }

    public void setDecision(String decision)
    {
        this.decision = decision;
    }

    public String getNotice()
    {
        return notice;
    }

    public void setNotice(String notice)
    {
        this.notice = notice;
    }
}
