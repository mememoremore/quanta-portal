package com.ruoyi.qt.domain;

/**
 * 面试轮次 qt_interview_round
 */
public class QtInterviewRound
{
    private Long roundId;
    private Integer roundNo;
    private String roundName;
    private String enabled;

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

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }
}
