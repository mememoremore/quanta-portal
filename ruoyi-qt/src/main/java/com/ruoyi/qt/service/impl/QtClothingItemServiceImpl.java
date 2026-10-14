package com.ruoyi.qt.service.impl;

import java.util.List;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.qt.cache.QtQueryCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.qt.mapper.QtClothingItemMapper;
import com.ruoyi.qt.domain.QtClothingItem;
import com.ruoyi.qt.service.IQtClothingItemService;

/**
 * 服装配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@Service
public class QtClothingItemServiceImpl implements IQtClothingItemService 
{
    @Autowired
    private QtClothingItemMapper qtClothingItemMapper;

    @Autowired
    private QtQueryCache qtQueryCache;

    /**
     * 查询服装配置
     * 
     * @param itemId 服装配置主键
     * @return 服装配置
     */
    @Override
    public QtClothingItem selectQtClothingItemByItemId(Long itemId)
    {
        return qtClothingItemMapper.selectQtClothingItemByItemId(itemId);
    }

    /**
     * 查询服装配置列表
     * 
     * @param qtClothingItem 服装配置
     * @return 服装配置
     */
    @Override
    public List<QtClothingItem> selectQtClothingItemList(QtClothingItem qtClothingItem)
    {
        return qtClothingItemMapper.selectQtClothingItemList(qtClothingItem);
    }

    /**
     * 新增服装配置
     * 
     * @param qtClothingItem 服装配置
     * @return 结果
     */
    @Override
    public int insertQtClothingItem(QtClothingItem qtClothingItem)
    {
        qtClothingItem.setCreateTime(DateUtils.getNowDate());
        int rows = qtClothingItemMapper.insertQtClothingItem(qtClothingItem);
        qtQueryCache.evict(CacheConstants.CACHE_QT_ITEM_LIST);
        return rows;
    }

    /**
     * 修改服装配置
     * 
     * @param qtClothingItem 服装配置
     * @return 结果
     */
    @Override
    public int updateQtClothingItem(QtClothingItem qtClothingItem)
    {
        qtClothingItem.setUpdateTime(DateUtils.getNowDate());
        int rows = qtClothingItemMapper.updateQtClothingItem(qtClothingItem);
        qtQueryCache.evict(CacheConstants.CACHE_QT_ITEM_LIST);
        return rows;
    }

    /**
     * 批量删除服装配置
     * 
     * @param itemIds 需要删除的服装配置主键
     * @return 结果
     */
    @Override
    public int deleteQtClothingItemByItemIds(Long[] itemIds)
    {
        int rows = qtClothingItemMapper.deleteQtClothingItemByItemIds(itemIds);
        qtQueryCache.evict(CacheConstants.CACHE_QT_ITEM_LIST);
        return rows;
    }

    /**
     * 删除服装配置信息
     * 
     * @param itemId 服装配置主键
     * @return 结果
     */
    @Override
    public int deleteQtClothingItemByItemId(Long itemId)
    {
        int rows = qtClothingItemMapper.deleteQtClothingItemByItemId(itemId);
        qtQueryCache.evict(CacheConstants.CACHE_QT_ITEM_LIST);
        return rows;
    }
}
