package com.ruoyi.qt.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 服装配置对象 qt_clothing_item
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtClothingItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 服装款式ID */
    private Long itemId;

    /** 款式名称 */
    @Excel(name = "款式名称")
    private String itemName;

    /** 服装效果图路径 */
    @Excel(name = "服装效果图路径")
    private String effectImagePath;

    /** 服装效果图完整URL */
    private String effectImageUrl;

    /** 可选颜色(JSON数组) */
    @Excel(name = "可选颜色(JSON数组)")
    private String colorOptionsJson;

    /** 可选尺码(JSON数组) */
    @Excel(name = "可选尺码(JSON数组)")
    private String sizeOptionsJson;

    /** 状态(0上架 1下架) */
    @Excel(name = "状态(0上架 1下架)")
    private String status;

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setItemName(String itemName) 
    {
        this.itemName = itemName;
    }

    public String getItemName() 
    {
        return itemName;
    }

    public void setEffectImagePath(String effectImagePath) 
    {
        this.effectImagePath = effectImagePath;
    }

    public String getEffectImagePath() 
    {
        return effectImagePath;
    }

    public String getEffectImageUrl()
    {
        return effectImageUrl;
    }

    public void setEffectImageUrl(String effectImageUrl)
    {
        this.effectImageUrl = effectImageUrl;
    }

    public void setColorOptionsJson(String colorOptionsJson) 
    {
        this.colorOptionsJson = colorOptionsJson;
    }

    public String getColorOptionsJson() 
    {
        return colorOptionsJson;
    }

    public void setSizeOptionsJson(String sizeOptionsJson) 
    {
        this.sizeOptionsJson = sizeOptionsJson;
    }

    public String getSizeOptionsJson() 
    {
        return sizeOptionsJson;
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
            .append("itemId", getItemId())
            .append("itemName", getItemName())
            .append("effectImagePath", getEffectImagePath())
            .append("effectImageUrl", getEffectImageUrl())
            .append("colorOptionsJson", getColorOptionsJson())
            .append("sizeOptionsJson", getSizeOptionsJson())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
