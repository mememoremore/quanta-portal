package com.ruoyi.qt.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 固定付款码配置对象 qt_payment_config
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtPaymentConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 支付配置ID */
    private Long configId;

    /** 支付配置名称 */
    @Excel(name = "支付配置名称")
    private String paymentName;

    /** 收款码图片路径(后台上传) */
    @Excel(name = "收款码图片路径(后台上传)")
    private String qrImagePath;

    /** 收款码图片完整URL */
    private String qrImageUrl;

    /** 启用状态(0停用 1启用) */
    @Excel(name = "启用状态(0停用 1启用)")
    private String enabled;

    public void setConfigId(Long configId) 
    {
        this.configId = configId;
    }

    public Long getConfigId() 
    {
        return configId;
    }

    public void setPaymentName(String paymentName) 
    {
        this.paymentName = paymentName;
    }

    public String getPaymentName() 
    {
        return paymentName;
    }

    public void setQrImagePath(String qrImagePath) 
    {
        this.qrImagePath = qrImagePath;
    }

    public String getQrImagePath() 
    {
        return qrImagePath;
    }

    public String getQrImageUrl()
    {
        return qrImageUrl;
    }

    public void setQrImageUrl(String qrImageUrl)
    {
        this.qrImageUrl = qrImageUrl;
    }

    public void setEnabled(String enabled) 
    {
        this.enabled = enabled;
    }

    public String getEnabled() 
    {
        return enabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("paymentName", getPaymentName())
            .append("qrImagePath", getQrImagePath())
            .append("qrImageUrl", getQrImageUrl())
            .append("enabled", getEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
