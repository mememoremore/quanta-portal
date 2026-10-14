package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtWorkstationReservation;

/**
 * 工位预约记录Service接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface IQtWorkstationReservationService 
{
    /**
     * 查询工位预约记录
     * 
     * @param reservationId 工位预约记录主键
     * @return 工位预约记录
     */
    public QtWorkstationReservation selectQtWorkstationReservationByReservationId(Long reservationId);

    /**
     * 查询工位预约记录列表
     * 
     * @param qtWorkstationReservation 工位预约记录
     * @return 工位预约记录集合
     */
    public List<QtWorkstationReservation> selectQtWorkstationReservationList(QtWorkstationReservation qtWorkstationReservation);

    /**
     * 查询工位预约记录详情列表
     *
     * @param qtWorkstationReservation 工位预约记录
     * @return 工位预约记录集合
     */
    public List<QtWorkstationReservation> selectQtWorkstationReservationDetailList(QtWorkstationReservation qtWorkstationReservation);

    /**
     * 新增工位预约记录
     * 
     * @param qtWorkstationReservation 工位预约记录
     * @return 结果
     */
    public int insertQtWorkstationReservation(QtWorkstationReservation qtWorkstationReservation);

    /**
     * 修改工位预约记录
     * 
     * @param qtWorkstationReservation 工位预约记录
     * @return 结果
     */
    public int updateQtWorkstationReservation(QtWorkstationReservation qtWorkstationReservation);

    /**
     * 批量删除工位预约记录
     * 
     * @param reservationIds 需要删除的工位预约记录主键集合
     * @return 结果
     */
    public int deleteQtWorkstationReservationByReservationIds(Long[] reservationIds);

    /**
     * 删除工位预约记录信息
     * 
     * @param reservationId 工位预约记录主键
     * @return 结果
     */
    public int deleteQtWorkstationReservationByReservationId(Long reservationId);
}
