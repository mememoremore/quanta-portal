package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtWorkstation;

/**
 * 实验室工位Service接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface IQtWorkstationService 
{
    /**
     * 查询实验室工位
     * 
     * @param workstationId 实验室工位主键
     * @return 实验室工位
     */
    public QtWorkstation selectQtWorkstationByWorkstationId(Long workstationId);

    /**
     * 查询实验室工位列表
     * 
     * @param qtWorkstation 实验室工位
     * @return 实验室工位集合
     */
    public List<QtWorkstation> selectQtWorkstationList(QtWorkstation qtWorkstation);

    /**
     * 新增实验室工位
     * 
     * @param qtWorkstation 实验室工位
     * @return 结果
     */
    public int insertQtWorkstation(QtWorkstation qtWorkstation);

    /**
     * 修改实验室工位
     * 
     * @param qtWorkstation 实验室工位
     * @return 结果
     */
    public int updateQtWorkstation(QtWorkstation qtWorkstation);

    /**
     * 批量删除实验室工位
     * 
     * @param workstationIds 需要删除的实验室工位主键集合
     * @return 结果
     */
    public int deleteQtWorkstationByWorkstationIds(Long[] workstationIds);

    /**
     * 删除实验室工位信息
     * 
     * @param workstationId 实验室工位主键
     * @return 结果
     */
    public int deleteQtWorkstationByWorkstationId(Long workstationId);
}
