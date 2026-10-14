package com.ruoyi.qt.mapper;

import java.util.List;
import com.ruoyi.qt.domain.QtPaymentConfig;

/**
 * 固定付款码配置Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface QtPaymentConfigMapper 
{
    /**
     * 查询固定付款码配置
     * 
     * @param configId 固定付款码配置主键
     * @return 固定付款码配置
     */
    public QtPaymentConfig selectQtPaymentConfigByConfigId(Long configId);

    /**
     * 查询固定付款码配置列表
     * 
     * @param qtPaymentConfig 固定付款码配置
     * @return 固定付款码配置集合
     */
    public List<QtPaymentConfig> selectQtPaymentConfigList(QtPaymentConfig qtPaymentConfig);

    /**
     * 新增固定付款码配置
     * 
     * @param qtPaymentConfig 固定付款码配置
     * @return 结果
     */
    public int insertQtPaymentConfig(QtPaymentConfig qtPaymentConfig);

    /**
     * 修改固定付款码配置
     * 
     * @param qtPaymentConfig 固定付款码配置
     * @return 结果
     */
    public int updateQtPaymentConfig(QtPaymentConfig qtPaymentConfig);

    /**
     * 删除固定付款码配置
     * 
     * @param configId 固定付款码配置主键
     * @return 结果
     */
    public int deleteQtPaymentConfigByConfigId(Long configId);

    /**
     * 批量删除固定付款码配置
     * 
     * @param configIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteQtPaymentConfigByConfigIds(Long[] configIds);
}
