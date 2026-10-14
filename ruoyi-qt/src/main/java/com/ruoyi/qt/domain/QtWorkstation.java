package com.ruoyi.qt.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 实验室工位对象 qt_workstation
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtWorkstation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 工位ID */
    private Long workstationId;

    /** 工位编号 */
    @Excel(name = "工位编号")
    private String workstationCode;

    /** 位置描述 */
    @Excel(name = "位置描述")
    private String locationDesc;

    /** 可容纳人数 */
    @Excel(name = "可容纳人数")
    private Long capacity;

    /** 状态(0可用 1停用) */
    @Excel(name = "状态(0可用 1停用)")
    private String status;

    public void setWorkstationId(Long workstationId) 
    {
        this.workstationId = workstationId;
    }

    public Long getWorkstationId() 
    {
        return workstationId;
    }

    public void setWorkstationCode(String workstationCode) 
    {
        this.workstationCode = workstationCode;
    }

    public String getWorkstationCode() 
    {
        return workstationCode;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("workstationId", getWorkstationId())
            .append("workstationCode", getWorkstationCode())
            .append("locationDesc", getLocationDesc())
            .append("capacity", getCapacity())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
