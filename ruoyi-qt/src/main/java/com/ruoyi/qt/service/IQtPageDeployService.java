package com.ruoyi.qt.service;

import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;

/**
 * 成员静态站点部署任务服务。
 *
 * requestDeploy 面向成员接口（带本人校验），enqueueLocked 面向已在事务中锁定站点行
 * 的内部调用方（创建/配置更新/后台任务），不依赖登录上下文。
 */
public interface IQtPageDeployService
{
    /** 成员手动部署：校验本人站点，分配新 request_version 并入队 PENDING 任务 */
    QtPageDeployment requestDeploy(Long siteId, String triggerType);

    /**
     * 在调用方已持有站点行锁的事务内入队：站点 request_version +1，
     * 以站点当前配置写部署快照；triggerType=MANUAL 时若已有同配置排队任务则直接返回它。
     */
    QtPageDeployment enqueueLocked(QtPageSite lockedSite, String triggerType);
}
