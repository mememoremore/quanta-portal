package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotNull;
import com.ruoyi.qt.validation.Create;

/**
 * 活动报名对象 qt_activity_signup
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtActivitySignup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 报名ID */
    private Long signupId;

    /** 活动ID */
    @NotNull(groups = Create.class, message = "活动ID不能为空")
    @Excel(name = "活动ID")
    private Long activityId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 状态(APPLIED/APPROVED/CANCELED/REJECTED) */
    @Excel(name = "状态(APPLIED/APPROVED/CANCELED/REJECTED)")
    private String status;

    /** 报名时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "报名时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date signupTime;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "取消时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date cancelTime;

    /** 活动标题 */
    @Excel(name = "活动标题")
    private String activityTitle;

    /** 活动开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "活动开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date activityStart;

    /** 活动结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "活动结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date activityEnd;

    /** 活动地点 */
    @Excel(name = "活动地点")
    private String locationDesc;

    /** 用户账号 */
    @Excel(name = "用户账号")
    private String userName;

    /** 用户昵称 */
    @Excel(name = "用户昵称")
    private String nickName;

    /** 成员编号 */
    @Excel(name = "成员编号")
    private String memberNo;

    /** 成员部门 */
    @Excel(name = "成员部门")
    private String memberDepartment;

    /** 成员职称 */
    @Excel(name = "成员职称")
    private String memberTitle;

    /** 成员届次 */
    @Excel(name = "成员届次")
    private String memberCohort;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 专业 */
    @Excel(name = "专业")
    private String major;

    /** 电话 */
    @Excel(name = "电话")
    private String phonenumber;

    /** 名额 */
    @Excel(name = "名额")
    private Long capacity;

    /** 活动类型 */
    private String activityType;

    public void setSignupId(Long signupId) 
    {
        this.signupId = signupId;
    }

    public Long getSignupId() 
    {
        return signupId;
    }

    public void setActivityId(Long activityId) 
    {
        this.activityId = activityId;
    }

    public Long getActivityId() 
    {
        return activityId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setSignupTime(Date signupTime) 
    {
        this.signupTime = signupTime;
    }

    public Date getSignupTime() 
    {
        return signupTime;
    }

    public void setCancelTime(Date cancelTime) 
    {
        this.cancelTime = cancelTime;
    }

    public Date getCancelTime() 
    {
        return cancelTime;
    }

    public String getActivityTitle()
    {
        return activityTitle;
    }

    public void setActivityTitle(String activityTitle)
    {
        this.activityTitle = activityTitle;
    }

    public Date getActivityStart()
    {
        return activityStart;
    }

    public void setActivityStart(Date activityStart)
    {
        this.activityStart = activityStart;
    }

    public Date getActivityEnd()
    {
        return activityEnd;
    }

    public void setActivityEnd(Date activityEnd)
    {
        this.activityEnd = activityEnd;
    }

    public String getLocationDesc()
    {
        return locationDesc;
    }

    public void setLocationDesc(String locationDesc)
    {
        this.locationDesc = locationDesc;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getMemberNo()
    {
        return memberNo;
    }

    public void setMemberNo(String memberNo)
    {
        this.memberNo = memberNo;
    }

    public String getMemberDepartment()
    {
        return memberDepartment;
    }

    public void setMemberDepartment(String memberDepartment)
    {
        this.memberDepartment = memberDepartment;
    }

    public String getMemberTitle()
    {
        return memberTitle;
    }

    public void setMemberTitle(String memberTitle)
    {
        this.memberTitle = memberTitle;
    }

    public String getMemberCohort()
    {
        return memberCohort;
    }

    public void setMemberCohort(String memberCohort)
    {
        this.memberCohort = memberCohort;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getMajor()
    {
        return major;
    }

    public void setMajor(String major)
    {
        this.major = major;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public Long getCapacity()
    {
        return capacity;
    }

    public void setCapacity(Long capacity)
    {
        this.capacity = capacity;
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
            .append("signupId", getSignupId())
            .append("activityId", getActivityId())
            .append("userId", getUserId())
            .append("status", getStatus())
            .append("signupTime", getSignupTime())
            .append("cancelTime", getCancelTime())
            .append("activityTitle", getActivityTitle())
            .append("activityStart", getActivityStart())
            .append("activityEnd", getActivityEnd())
            .append("locationDesc", getLocationDesc())
            .append("userName", getUserName())
            .append("nickName", getNickName())
            .append("memberNo", getMemberNo())
            .append("memberDepartment", getMemberDepartment())
            .append("memberTitle", getMemberTitle())
            .append("memberCohort", getMemberCohort())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
