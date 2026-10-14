package com.ruoyi.qt.mapper;

import java.util.List;
import com.ruoyi.qt.domain.QtActivity;

/**
 * 实验室活动Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface QtActivityMapper 
{
    /**
     * 查询实验室活动
     * 
     * @param activityId 实验室活动主键
     * @return 实验室活动
     */
    public QtActivity selectQtActivityByActivityId(Long activityId);

    /**
     * 查询实验室活动列表
     * 
     * @param qtActivity 实验室活动
     * @return 实验室活动集合
     */
    public List<QtActivity> selectQtActivityList(QtActivity qtActivity);

    /**
     * 新增实验室活动
     * 
     * @param qtActivity 实验室活动
     * @return 结果
     */
    public int insertQtActivity(QtActivity qtActivity);

    /**
     * 修改实验室活动
     * 
     * @param qtActivity 实验室活动
     * @return 结果
     */
    public int updateQtActivity(QtActivity qtActivity);

    /**
     * 删除实验室活动
     * 
     * @param activityId 实验室活动主键
     * @return 结果
     */
    public int deleteQtActivityByActivityId(Long activityId);

    /**
     * 批量删除实验室活动
     * 
     * @param activityIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteQtActivityByActivityIds(Long[] activityIds);
}
