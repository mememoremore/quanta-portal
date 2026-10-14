package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 实验室活动对象 qt_activity
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtActivity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 活动标题 */
    @Excel(name = "活动标题")
    private String title;

    /** 活动描述 */
    @Excel(name = "活动描述")
    private String description;

    /** 报名开始 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "报名开始", width = 30, dateFormat = "yyyy-MM-dd")
    private Date signupStart;

    /** 报名截止 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "报名截止", width = 30, dateFormat = "yyyy-MM-dd")
    private Date signupEnd;

    /** 活动开始 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "活动开始", width = 30, dateFormat = "yyyy-MM-dd")
    private Date activityStart;

    /** 活动结束 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "活动结束", width = 30, dateFormat = "yyyy-MM-dd")
    private Date activityEnd;

    /** 活动地点 */
    @Excel(name = "活动地点")
    private String locationDesc;

    /** 名额 */
    @Excel(name = "名额")
    private Long capacity;

    /** 状态(DRAFT/PUBLISHED/CANCELED/DELETED) */
    @Excel(name = "状态(DRAFT/PUBLISHED/CANCELED/DELETED)")
    private String status;

    /** 创建人 */
    @Excel(name = "创建人")
    private Long creatorUserId;

    /** 活动类型(LECTURE/SHARING/GENERAL) */
    @Excel(name = "活动类型")
    private String activityType;

    public void setActivityId(Long activityId) 
    {
        this.activityId = activityId;
    }

    public Long getActivityId() 
    {
        return activityId;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setSignupStart(Date signupStart) 
    {
        this.signupStart = signupStart;
    }

    public Date getSignupStart() 
    {
        return signupStart;
    }

    public void setSignupEnd(Date signupEnd) 
    {
        this.signupEnd = signupEnd;
    }

    public Date getSignupEnd() 
    {
        return signupEnd;
    }

    public void setActivityStart(Date activityStart) 
    {
        this.activityStart = activityStart;
    }

    public Date getActivityStart() 
    {
        return activityStart;
    }

    public void setActivityEnd(Date activityEnd) 
    {
        this.activityEnd = activityEnd;
    }

    public Date getActivityEnd() 
    {
        return activityEnd;
    }

    public void setLocationDesc(String locationDesc) 
    {
        this.locationDesc = locationDesc;
    }

    public String getLocationDesc() 
    {
        return locationDesc;
    }

    public void setCapacity(Long capacity) 
    {
        this.capacity = capacity;
    }

    public Long getCapacity() 
    {
        return capacity;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setCreatorUserId(Long creatorUserId) 
    {
        this.creatorUserId = creatorUserId;
    }

    public Long getCreatorUserId()
    {
        return creatorUserId;
    }

    public String getActivityType()
    {
        return activityType;
    }

    public void setActivityType(String activityType)
    {
        this.activityType = activityType;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("activityId", getActivityId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("signupStart", getSignupStart())
            .append("signupEnd", getSignupEnd())
            .append("activityStart", getActivityStart())
            .append("activityEnd", getActivityEnd())
            .append("locationDesc", getLocationDesc())
            .append("capacity", getCapacity())
            .append("status", getStatus())
            .append("creatorUserId", getCreatorUserId())
            .append("activityType", getActivityType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
