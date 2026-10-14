package com.ruoyi.qt.service.impl;

import java.util.List;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.ruoyi.qt.mapper.QtPaymentConfigMapper;
import com.ruoyi.qt.domain.QtPaymentConfig;
import com.ruoyi.qt.service.IQtPaymentConfigService;

/**
 * 固定付款码配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@Service
public class QtPaymentConfigServiceImpl implements IQtPaymentConfigService 
{
    @Autowired
    private QtPaymentConfigMapper qtPaymentConfigMapper;

    /**
     * 查询固定付款码配置
     * 
     * @param configId 固定付款码配置主键
     * @return 固定付款码配置
     */
    @Override
    @Cacheable(cacheNames = CacheConstants.CACHE_QT_PAYMENT_CONFIG, key = "'detail:' + #configId")
    public QtPaymentConfig selectQtPaymentConfigByConfigId(Long configId)
    {
        return qtPaymentConfigMapper.selectQtPaymentConfigByConfigId(configId);
    }

    /**
     * 查询固定付款码配置列表
     * 
     * @param qtPaymentConfig 固定付款码配置
     * @return 固定付款码配置
     */
    @Override
    public List<QtPaymentConfig> selectQtPaymentConfigList(QtPaymentConfig qtPaymentConfig)
    {
        return qtPaymentConfigMapper.selectQtPaymentConfigList(qtPaymentConfig);
    }

    /**
     * 新增固定付款码配置
     * 
     * @param qtPaymentConfig 固定付款码配置
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_PAYMENT_CONFIG, allEntries = true)
    public int insertQtPaymentConfig(QtPaymentConfig qtPaymentConfig)
    {
        qtPaymentConfig.setCreateTime(DateUtils.getNowDate());
        return qtPaymentConfigMapper.insertQtPaymentConfig(qtPaymentConfig);
    }

    /**
     * 修改固定付款码配置
     * 
     * @param qtPaymentConfig 固定付款码配置
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_PAYMENT_CONFIG, allEntries = true)
    public int updateQtPaymentConfig(QtPaymentConfig qtPaymentConfig)
    {
        qtPaymentConfig.setUpdateTime(DateUtils.getNowDate());
        qtPaymentConfig.setUpdateBy(SecurityUtils.getUsername());
        return qtPaymentConfigMapper.updateQtPaymentConfig(qtPaymentConfig);
    }

    /**
     * 批量删除固定付款码配置
     * 
     * @param configIds 需要删除的固定付款码配置主键
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_PAYMENT_CONFIG, allEntries = true)
    public int deleteQtPaymentConfigByConfigIds(Long[] configIds)
    {
        return qtPaymentConfigMapper.deleteQtPaymentConfigByConfigIds(configIds);
    }

    /**
     * 删除固定付款码配置信息
     * 
     * @param configId 固定付款码配置主键
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_PAYMENT_CONFIG, allEntries = true)
    public int deleteQtPaymentConfigByConfigId(Long configId)
    {
        return qtPaymentConfigMapper.deleteQtPaymentConfigByConfigId(configId);
    }
}
