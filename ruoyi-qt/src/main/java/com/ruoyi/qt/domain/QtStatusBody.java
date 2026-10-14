package com.ruoyi.qt.domain;

/**
 * 通用状态更新请求体
 */
public class QtStatusBody
{
    private String joinStatus;
    private String finalStatus;
    private String remark;

    public String getJoinStatus()
    {
        return joinStatus;
    }

    public void setJoinStatus(String joinStatus)
    {
        this.joinStatus = joinStatus;
    }

    public String getFinalStatus()
    {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus)
    {
        this.finalStatus = finalStatus;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
