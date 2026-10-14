package com.ruoyi.qt.mapper;

import java.util.List;
import com.ruoyi.qt.domain.QtActivitySignup;

/**
 * 活动报名Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface QtActivitySignupMapper 
{
    /**
     * 查询活动报名
     * 
     * @param signupId 活动报名主键
     * @return 活动报名
     */
    public QtActivitySignup selectQtActivitySignupBySignupId(Long signupId);

    /**
     * 查询活动报名列表
     * 
     * @param qtActivitySignup 活动报名
     * @return 活动报名集合
     */
    public List<QtActivitySignup> selectQtActivitySignupList(QtActivitySignup qtActivitySignup);

    /**
     * 查询活动报名详情列表
     *
     * @param qtActivitySignup 活动报名
     * @return 活动报名集合
     */
    public List<QtActivitySignup> selectQtActivitySignupDetailList(QtActivitySignup qtActivitySignup);

    /**
     * 新增活动报名
     * 
     * @param qtActivitySignup 活动报名
     * @return 结果
     */
    public int insertQtActivitySignup(QtActivitySignup qtActivitySignup);

    /**
     * 修改活动报名
     * 
     * @param qtActivitySignup 活动报名
     * @return 结果
     */
    public int updateQtActivitySignup(QtActivitySignup qtActivitySignup);

    /**
     * 删除活动报名
     * 
     * @param signupId 活动报名主键
     * @return 结果
     */
    public int deleteQtActivitySignupBySignupId(Long signupId);

    /**
     * 批量删除活动报名
     * 
     * @param signupIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteQtActivitySignupBySignupIds(Long[] signupIds);
}
