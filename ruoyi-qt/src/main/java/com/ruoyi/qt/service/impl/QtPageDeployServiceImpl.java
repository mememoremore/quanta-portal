package com.ruoyi.qt.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtPageDeploymentMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtPagesAccess;
import com.ruoyi.qt.service.IQtPageDeployService;

/**
 * 部署任务入队实现。
 *
 * 入队是短事务动作：锁定站点行、推进 request_version、写 PENDING 快照；
 * 下载/发布等执行动作由 Task 5 的 Worker 完成，不占用数据库行锁。
 */
@Service
public class QtPageDeployServiceImpl implements IQtPageDeployService
{
    @Autowired
    private QtPageSiteMapper siteMapper;

    @Autowired
    private QtPageDeploymentMapper deploymentMapper;

    @Autowired
    private QtPagesAccess access;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public QtPageDeployment requestDeploy(Long siteId, String triggerType)
    {
        QtPageSite site = access.requireOwnedSite(siteId);
        return enqueueLocked(site, triggerType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public QtPageDeployment enqueueLocked(QtPageSite lockedSite, String triggerType)
    {
        if (lockedSite == null || lockedSite.getSiteId() == null)
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        // 重新取行锁：调用方可能在未加锁的路径进来；同一事务内重复锁定无副作用
        QtPageSite site = siteMapper.selectForUpdate(lockedSite.getSiteId());
        if (site == null)
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        if (QtPageDeployment.TRIGGER_MANUAL.equals(triggerType))
        {
            QtPageDeployment reusable = deploymentMapper.selectReusableManual(site.getSiteId());
            if (reusable != null && snapshotMatchesCurrentConfig(reusable, site))
            {
                return reusable;
            }
        }
        long requestVersion = site.getRequestVersion() + 1;
        site.setRequestVersion(requestVersion);
        siteMapper.updateSite(site);
        // 新意图成立后，旧的非终态目标一律失效；发布前 Worker 仍会复核版本
        deploymentMapper.markSupersededBefore(site.getSiteId(), requestVersion);

        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setSiteId(site.getSiteId());
        deployment.setRequestVersion(requestVersion);
        deployment.setConfigVersion(site.getConfigVersion());
        deployment.setRepoOwner(site.getRepoOwner());
        deployment.setRepoName(site.getRepoName());
        deployment.setBranch(site.getBranch());
        deployment.setPublishDir(site.getPublishDir());
        deployment.setTriggerType(triggerType);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        deployment.setAttemptCount(0);
        deployment.setReleaseAvailable(false);
        deploymentMapper.insertDeployment(deployment);
        // 普通部署的目标实体目录以自身 deploymentId 命名
        deployment.setReleaseId(deployment.getDeploymentId());
        deploymentMapper.updateDeployment(deployment);
        return deployment;
    }

    private boolean snapshotMatchesCurrentConfig(QtPageDeployment deployment, QtPageSite site)
    {
        return equalsNullable(deployment.getRepoOwner(), site.getRepoOwner())
                && equalsNullable(deployment.getRepoName(), site.getRepoName())
                && equalsNullable(deployment.getBranch(), site.getBranch())
                && equalsNullable(deployment.getPublishDir(), site.getPublishDir());
    }

    private static boolean equalsNullable(Object left, Object right)
    {
        return left == null ? right == null : left.equals(right);
    }
}
