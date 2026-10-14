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
 * 工位预约记录对象 qt_workstation_reservation
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtWorkstationReservation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预约ID */
    private Long reservationId;

    /** 工位ID */
    @NotNull(groups = Create.class, message = "工位ID不能为空")
    @Excel(name = "工位ID")
    private Long workstationId;

    /** 用户ID(sys_user.user_id) */
    @Excel(name = "用户ID(sys_user.user_id)")
    private Long userId;

    /** 预约开始时间 */
    @NotNull(groups = Create.class, message = "预约开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预约开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date reserveStart;

    /** 预约结束时间 */
    @NotNull(groups = Create.class, message = "预约结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预约结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date reserveEnd;

    /** 状态(PENDING/APPROVED/CANCELED/FINISHED) */
    @Excel(name = "状态(PENDING/APPROVED/CANCELED/FINISHED)")
    private String status;

    /** 用途 */
    @Excel(name = "用途")
    private String purpose;

    /** 工位编号 */
    @Excel(name = "工位编号")
    private String workstationCode;

    /** 工位位置 */
    @Excel(name = "工位位置")
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

    public void setReservationId(Long reservationId) 
    {
        this.reservationId = reservationId;
    }

    public Long getReservationId() 
    {
        return reservationId;
    }

    public void setWorkstationId(Long workstationId) 
    {
        this.workstationId = workstationId;
    }

    public Long getWorkstationId() 
    {
        return workstationId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setReserveStart(Date reserveStart) 
    {
        this.reserveStart = reserveStart;
    }

    public Date getReserveStart() 
    {
        return reserveStart;
    }

    public void setReserveEnd(Date reserveEnd) 
    {
        this.reserveEnd = reserveEnd;
    }

    public Date getReserveEnd() 
    {
        return reserveEnd;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setPurpose(String purpose) 
    {
        this.purpose = purpose;
    }

    public String getPurpose() 
    {
        return purpose;
    }

    public String getWorkstationCode()
    {
        return workstationCode;
    }

    public void setWorkstationCode(String workstationCode)
    {
        this.workstationCode = workstationCode;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reservationId", getReservationId())
            .append("workstationId", getWorkstationId())
            .append("userId", getUserId())
            .append("reserveStart", getReserveStart())
            .append("reserveEnd", getReserveEnd())
            .append("status", getStatus())
            .append("purpose", getPurpose())
            .append("workstationCode", getWorkstationCode())
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
