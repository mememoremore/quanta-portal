package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtClothingOrder;

/**
 * 实验室服装订单Service接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface IQtClothingOrderService 
{
    /**
     * 查询实验室服装订单
     * 
     * @param orderId 实验室服装订单主键
     * @return 实验室服装订单
     */
    public QtClothingOrder selectQtClothingOrderByOrderId(Long orderId);

    /**
     * 查询实验室服装订单列表
     * 
     * @param qtClothingOrder 实验室服装订单
     * @return 实验室服装订单集合
     */
    public List<QtClothingOrder> selectQtClothingOrderList(QtClothingOrder qtClothingOrder);

    /**
     * 查询实验室服装订单详情列表
     *
     * @param qtClothingOrder 实验室服装订单
     * @return 实验室服装订单集合
     */
    public List<QtClothingOrder> selectQtClothingOrderDetailList(QtClothingOrder qtClothingOrder);

    /**
     * 新增实验室服装订单
     * 
     * @param qtClothingOrder 实验室服装订单
     * @return 结果
     */
    public int insertQtClothingOrder(QtClothingOrder qtClothingOrder);

    /**
     * 修改实验室服装订单
     * 
     * @param qtClothingOrder 实验室服装订单
     * @return 结果
     */
    public int updateQtClothingOrder(QtClothingOrder qtClothingOrder);

    /**
     * 批量删除实验室服装订单
     * 
     * @param orderIds 需要删除的实验室服装订单主键集合
     * @return 结果
     */
    public int deleteQtClothingOrderByOrderIds(Long[] orderIds);

    /**
     * 删除实验室服装订单信息
     * 
     * @param orderId 实验室服装订单主键
     * @return 结果
     */
    public int deleteQtClothingOrderByOrderId(Long orderId);

    /**
     * 确认收款
     */
    public int approveOrder(Long orderId, String operator);
}
