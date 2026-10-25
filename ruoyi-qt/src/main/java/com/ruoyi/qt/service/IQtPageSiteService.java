package com.ruoyi.qt.service;

import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.domain.QtPageSiteCreateBody;
import com.ruoyi.qt.domain.QtPageSiteSaveResult;
import com.ruoyi.qt.domain.QtPageSiteUpdateBody;

/**
 * 成员静态站点服务。所有方法都要求当前登录用户为有效塔员，且只能操作本人站点。
 */
public interface IQtPageSiteService
{
    /** 创建本人站点并入队首次部署；slug 冲突返回 409，同事务返回站点与部署任务 */
    QtPageSiteSaveResult create(QtPageSiteCreateBody body);

    /** 本人站点；尚未创建时返回 null（正常空状态） */
    QtPageSite getOwnSite();

    /** 更新仓库/分支/发布目录：推进 config_version 与 request_version，旧目标失效并入队 */
    QtPageSiteSaveResult update(Long siteId, QtPageSiteUpdateBody body);
}
