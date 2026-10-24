package com.ruoyi.qt.service;

import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;

/**
 * 成员静态站点部署任务服务。
 *
 * requestDeploy 面向成员接口（带本人校验），enqueueLocked 面向已在事务中锁定站点行
 * 的内部调用方（创建/配置更新/后台任务），不依赖登录上下文。执行与恢复由调度组件触发，
 * 持久化任务表是唯一事实来源。
 */
public interface IQtPageDeployService
{
    /** 成员手动部署：校验本人站点，分配新 request_version 并入队 PENDING 任务 */
    QtPageDeployment requestDeploy(Long siteId, String triggerType);

    /**
     * 在调用方已持有站点行锁的事务内入队：站点 request_version 加一，
     * 以站点当前配置写部署快照；triggerType 为 MANUAL 时若已有同配置排队任务则直接返回它。
     */
    QtPageDeployment enqueueLocked(QtPageSite lockedSite, String triggerType);

    /** 轮询到期的启用站点（网络请求不占用数据库事务），新分支头入队 POLL 意图 */
    void pollDueSites();

    /** 扫描到期 PENDING 任务并提交到部署执行器；拒绝时任务保持 PENDING */
    void dispatchPending();

    /** 运行单个部署任务（版本复核、受限下载、安全解包、原子发布） */
    void runDeployment(long deploymentId);

    /** 启动恢复：把中断在 RESOLVING/DOWNLOADING/VALIDATING 的任务按剩余预算重新排队 */
    void recoverInterrupted();

    /** 启动恢复：对账 PUBLISHING 中断任务与磁盘真实 current，不伪报成功、不倒退新目标 */
    void reconcilePublishing();
}
