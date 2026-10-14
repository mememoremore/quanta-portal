package com.ruoyi.qt.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.qt.mapper.QtActivitySignupMapper;
import com.ruoyi.qt.domain.QtActivitySignup;
import com.ruoyi.qt.service.IQtActivitySignupService;

/**
 * 活动报名Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@Service
public class QtActivitySignupServiceImpl implements IQtActivitySignupService 
{
    @Autowired
    private QtActivitySignupMapper qtActivitySignupMapper;

    /**
     * 查询活动报名
     * 
     * @param signupId 活动报名主键
     * @return 活动报名
     */
    @Override
    public QtActivitySignup selectQtActivitySignupBySignupId(Long signupId)
    {
        return qtActivitySignupMapper.selectQtActivitySignupBySignupId(signupId);
    }

    /**
     * 查询活动报名列表
     * 
     * @param qtActivitySignup 活动报名
     * @return 活动报名
     */
    @Override
    public List<QtActivitySignup> selectQtActivitySignupList(QtActivitySignup qtActivitySignup)
    {
        return qtActivitySignupMapper.selectQtActivitySignupList(qtActivitySignup);
    }

    @Override
    public List<QtActivitySignup> selectQtActivitySignupDetailList(QtActivitySignup qtActivitySignup)
    {
        return qtActivitySignupMapper.selectQtActivitySignupDetailList(qtActivitySignup);
    }

    /**
     * 新增活动报名
     * 
     * @param qtActivitySignup 活动报名
     * @return 结果
     */
    @Override
    public int insertQtActivitySignup(QtActivitySignup qtActivitySignup)
    {
        qtActivitySignup.setCreateTime(DateUtils.getNowDate());
        return qtActivitySignupMapper.insertQtActivitySignup(qtActivitySignup);
    }

    /**
     * 修改活动报名
     * 
     * @param qtActivitySignup 活动报名
     * @return 结果
     */
    @Override
    public int updateQtActivitySignup(QtActivitySignup qtActivitySignup)
    {
        qtActivitySignup.setUpdateTime(DateUtils.getNowDate());
        return qtActivitySignupMapper.updateQtActivitySignup(qtActivitySignup);
    }

    /**
     * 批量删除活动报名
     * 
     * @param signupIds 需要删除的活动报名主键
     * @return 结果
     */
    @Override
    public int deleteQtActivitySignupBySignupIds(Long[] signupIds)
    {
        return qtActivitySignupMapper.deleteQtActivitySignupBySignupIds(signupIds);
    }

    /**
     * 删除活动报名信息
     * 
     * @param signupId 活动报名主键
     * @return 结果
     */
    @Override
    public int deleteQtActivitySignupBySignupId(Long signupId)
    {
        return qtActivitySignupMapper.deleteQtActivitySignupBySignupId(signupId);
    }
}
