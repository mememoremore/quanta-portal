package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员届次档案
 */
public class QtMemberRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long userId;
    private Long cohortId;
    private String roleCategory;
    private String memberStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date joinTime;
    private String retainFlag;
    private String cohortName;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getCohortId()
    {
        return cohortId;
    }

    public void setCohortId(Long cohortId)
    {
        this.cohortId = cohortId;
    }

    public String getRoleCategory()
    {
        return roleCategory;
    }

    public void setRoleCategory(String roleCategory)
    {
        this.roleCategory = roleCategory;
    }

    public String getMemberStatus()
    {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus)
    {
        this.memberStatus = memberStatus;
    }

    public Date getJoinTime()
    {
        return joinTime;
    }

    public void setJoinTime(Date joinTime)
    {
        this.joinTime = joinTime;
    }

    public String getRetainFlag()
    {
        return retainFlag;
    }

    public void setRetainFlag(String retainFlag)
    {
        this.retainFlag = retainFlag;
    }

    public String getCohortName()
    {
        return cohortName;
    }

    public void setCohortName(String cohortName)
    {
        this.cohortName = cohortName;
    }
}
