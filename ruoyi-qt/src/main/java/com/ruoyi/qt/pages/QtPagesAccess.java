package com.ruoyi.qt.pages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtLabMemberMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.util.QtAuthUtils;

/**
 * Pages 本人授权入口。
 *
 * 与项目内其他成员业务不同：这里没有管理员旁路——CEO/管理员也不能通过本人接口
 * 操作他人站点；每个请求都要求登录（401）、有效塔员（403）与站点所有权（不满足
 * 时与“站点不存在”一样返回 404，避免暴露他人站点存在）。
 */
@Component
public class QtPagesAccess
{
    @Autowired
    private QtPageSiteMapper siteMapper;

    @Autowired
    private QtLabMemberMapper labMemberMapper;

    /**
     * 校验当前登录用户是有效塔员并返回其 userId。
     * 未登录 401；登录用户不是塔员，或数据库中已停用/除名，返回 403。
     */
    public long requireCurrentMemberId()
    {
        QtAuthUtils.requireQuantaMember();
        Long userId = SecurityUtils.getUserId();
        if (userId == null || labMemberMapper.selectVisibleQuantaMember(userId) == null)
        {
            throw new ServiceException("仅有效塔员可使用个人网站功能", HttpStatus.FORBIDDEN);
        }
        return userId;
    }

    /**
     * 校验当前登录用户是有效塔员，且 siteId 对应自己拥有的站点；
     * 站点不存在或属于他人时统一 404。
     */
    public QtPageSite requireOwnedSite(Long siteId)
    {
        long userId = requireCurrentMemberId();
        QtPageSite site = siteId == null ? null : siteMapper.selectById(siteId);
        if (site == null || site.getUserId() == null || !site.getUserId().equals(userId))
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        return site;
    }
}
