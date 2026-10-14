package com.ruoyi.qt.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtWorkstationReservation;

/**
 * 工位预约记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface QtWorkstationReservationMapper 
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

    public int countOverlapping(@Param("workstationId") Long workstationId, @Param("reserveStart") Date reserveStart,
            @Param("reserveEnd") Date reserveEnd, @Param("excludeId") Long excludeId);

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
     * 删除工位预约记录
     * 
     * @param reservationId 工位预约记录主键
     * @return 结果
     */
    public int deleteQtWorkstationReservationByReservationId(Long reservationId);

    /**
     * 批量删除工位预约记录
     * 
     * @param reservationIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteQtWorkstationReservationByReservationIds(Long[] reservationIds);
}
