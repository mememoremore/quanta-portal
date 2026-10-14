package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtClothingItem;

/**
 * 服装配置Service接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface IQtClothingItemService 
{
    /**
     * 查询服装配置
     * 
     * @param itemId 服装配置主键
     * @return 服装配置
     */
    public QtClothingItem selectQtClothingItemByItemId(Long itemId);

    /**
     * 查询服装配置列表
     * 
     * @param qtClothingItem 服装配置
     * @return 服装配置集合
     */
    public List<QtClothingItem> selectQtClothingItemList(QtClothingItem qtClothingItem);

    /**
     * 新增服装配置
     * 
     * @param qtClothingItem 服装配置
     * @return 结果
     */
    public int insertQtClothingItem(QtClothingItem qtClothingItem);

    /**
     * 修改服装配置
     * 
     * @param qtClothingItem 服装配置
     * @return 结果
     */
    public int updateQtClothingItem(QtClothingItem qtClothingItem);

    /**
     * 批量删除服装配置
     * 
     * @param itemIds 需要删除的服装配置主键集合
     * @return 结果
     */
    public int deleteQtClothingItemByItemIds(Long[] itemIds);

    /**
     * 删除服装配置信息
     * 
     * @param itemId 服装配置主键
     * @return 结果
     */
    public int deleteQtClothingItemByItemId(Long itemId);
}
