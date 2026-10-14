package com.ruoyi.qt.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtWorkstationReservation;
import com.ruoyi.qt.mapper.QtWorkstationReservationMapper;
import com.ruoyi.qt.service.IQtWorkstationReservationService;

/**
 * 工位预约记录Service业务层处理
 */
@Service
public class QtWorkstationReservationServiceImpl implements IQtWorkstationReservationService
{
    @Autowired
    private QtWorkstationReservationMapper qtWorkstationReservationMapper;

    @Override
    public QtWorkstationReservation selectQtWorkstationReservationByReservationId(Long reservationId)
    {
        return qtWorkstationReservationMapper.selectQtWorkstationReservationByReservationId(reservationId);
    }

    @Override
    public List<QtWorkstationReservation> selectQtWorkstationReservationList(QtWorkstationReservation qtWorkstationReservation)
    {
        return qtWorkstationReservationMapper.selectQtWorkstationReservationList(qtWorkstationReservation);
    }

    @Override
    public List<QtWorkstationReservation> selectQtWorkstationReservationDetailList(QtWorkstationReservation qtWorkstationReservation)
    {
        return qtWorkstationReservationMapper.selectQtWorkstationReservationDetailList(qtWorkstationReservation);
    }

    @Override
    @Transactional
    public int insertQtWorkstationReservation(QtWorkstationReservation reservation)
    {
        assertSlot(reservation);
        if (StringUtils.isEmpty(reservation.getStatus()))
        {
            reservation.setStatus("PENDING");
        }
        assertNoOverlap(reservation.getWorkstationId(), reservation.getReserveStart(), reservation.getReserveEnd(), null);
        reservation.setCreateTime(DateUtils.getNowDate());
        return qtWorkstationReservationMapper.insertQtWorkstationReservation(reservation);
    }

    @Override
    @Transactional
    public int updateQtWorkstationReservation(QtWorkstationReservation reservation)
    {
        QtWorkstationReservation old = qtWorkstationReservationMapper.selectQtWorkstationReservationByReservationId(reservation.getReservationId());
        if (old == null)
        {
            throw new ServiceException("预约记录不存在");
        }
        Long workstationId = reservation.getWorkstationId() != null ? reservation.getWorkstationId() : old.getWorkstationId();
        Date start = reservation.getReserveStart() != null ? reservation.getReserveStart() : old.getReserveStart();
        Date end = reservation.getReserveEnd() != null ? reservation.getReserveEnd() : old.getReserveEnd();
        String status = StringUtils.isNotEmpty(reservation.getStatus()) ? reservation.getStatus() : old.getStatus();
        if (!"CANCELED".equals(status) && !"FINISHED".equals(status))
        {
            assertNoOverlap(workstationId, start, end, reservation.getReservationId());
        }
        reservation.setUpdateTime(DateUtils.getNowDate());
        return qtWorkstationReservationMapper.updateQtWorkstationReservation(reservation);
    }

    @Override
    public int deleteQtWorkstationReservationByReservationIds(Long[] reservationIds)
    {
        return qtWorkstationReservationMapper.deleteQtWorkstationReservationByReservationIds(reservationIds);
    }

    @Override
    public int deleteQtWorkstationReservationByReservationId(Long reservationId)
    {
        return qtWorkstationReservationMapper.deleteQtWorkstationReservationByReservationId(reservationId);
    }

    private void assertSlot(QtWorkstationReservation reservation)
    {
        if (reservation.getWorkstationId() == null)
        {
            throw new ServiceException("工位ID不能为空");
        }
        if (reservation.getReserveStart() == null || reservation.getReserveEnd() == null)
        {
            throw new ServiceException("预约起止时间不能为空");
        }
        if (!reservation.getReserveEnd().after(reservation.getReserveStart()))
        {
            throw new ServiceException("预约结束时间必须晚于开始时间");
        }
    }

    private void assertNoOverlap(Long workstationId, Date start, Date end, Long excludeId)
    {
        if (workstationId == null || start == null || end == null)
        {
            return;
        }
        if (qtWorkstationReservationMapper.countOverlapping(workstationId, start, end, excludeId) > 0)
        {
            throw new ServiceException("该工位在此时间段已被预约");
        }
    }
}
