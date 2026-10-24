package com.ruoyi.qt.service.impl;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.qt.config.QtPagesProperties;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtPageDeploymentMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtGitHubClient;
import com.ruoyi.qt.pages.QtPageReleaseStore;
import com.ruoyi.qt.pages.QtPagesRuntime;

/**
 * Pages 部署相关的短事务单元。全部方法经 Spring 代理开启事务，
 * 由 QtPageDeployServiceImpl 在事务外完成网络与文件工作后调用。
 */
@Component
public class QtPageDeployTransactions
{
    @Autowired
    private QtPageSiteMapper siteMapper;

    @Autowired
    private QtPageDeploymentMapper deploymentMapper;

    @Autowired
    private QtPagesRuntime runtime;

    // ---------- 轮询 ----------

    /**
     * 轮询结果落库：新目标入队 POLL 意图；目标未变或已有同配置未解析意图时只记录观察。
     * 回滚不受未变分支头影响（desired_sha/current_sha 分开比较）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyPollHead(Long siteId, QtGitHubClient.Head head, Date now, Date nextPoll)
    {
        QtPageSite site = siteMapper.selectForUpdate(siteId);
        if (site == null || !QtPageSite.STATUS_ENABLED.equals(site.getStatus()))
        {
            return;
        }
        site.setLastCheckedTime(now);
        site.setLastCheckError(null);
        if (head.notModified())
        {
            site.setNextPollTime(nextPoll);
            siteMapper.updateSite(site);
            return;
        }
        site.setLastObservedSha(head.sha());
        if (head.etag() != null)
        {
            site.setPollEtag(head.etag());
        }
        boolean sameTarget = head.sha().equals(site.getDesiredSha())
                || head.sha().equals(site.getCurrentSha());
        boolean hasUnresolvedIntent = !sameTarget
                && deploymentMapper.selectUnresolvedForSite(site.getSiteId()) != null;
        if (sameTarget || hasUnresolvedIntent)
        {
            site.setDesiredSha(head.sha());
            site.setNextPollTime(nextPoll);
            siteMapper.updateSite(site);
            return;
        }
        long requestVersion = site.getRequestVersion() + 1;
        site.setRequestVersion(requestVersion);
        site.setDesiredSha(head.sha());
        site.setNextPollTime(nextPoll);
        siteMapper.updateSite(site);
        deploymentMapper.markSupersededBefore(site.getSiteId(), requestVersion);
        QtPageDeployment deployment = newDeployment(site, QtPageDeployment.TRIGGER_POLL, requestVersion);
        deployment.setCommitSha(head.sha());
        deploymentMapper.insertDeployment(deployment);
        deployment.setReleaseId(deployment.getDeploymentId());
        deploymentMapper.updateDeployment(deployment);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordPollFailure(Long siteId, String errorCode, String message, Date now, Date nextPoll)
    {
        QtPageSite site = siteMapper.selectForUpdate(siteId);
        if (site == null)
        {
            return;
        }
        site.setLastCheckedTime(now);
        site.setLastCheckError(truncate((errorCode == null ? "ERROR" : errorCode) + ": " + message, 1000));
        site.setNextPollTime(nextPoll);
        siteMapper.updateSite(site);
    }

    // ---------- 领取与回退 ----------

    @Transactional(rollbackFor = Exception.class)
    public boolean claim(long deploymentId)
    {
        QtPageDeployment deployment = deploymentMapper.selectForUpdate(deploymentId);
        if (deployment == null || !QtPageDeployment.STATUS_PENDING.equals(deployment.getStatus()))
        {
            return false;
        }
        deployment.setStatus(QtPageDeployment.STATUS_RESOLVING);
        deploymentMapper.updateDeployment(deployment);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void unclaim(long deploymentId, Date now)
    {
        QtPageDeployment deployment = deploymentMapper.selectForUpdate(deploymentId);
        if (deployment == null || !QtPageDeployment.STATUS_RESOLVING.equals(deployment.getStatus()))
        {
            return;
        }
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        deployment.setNextAttemptTime(now);
        deploymentMapper.updateDeployment(deployment);
    }

    /** 领取或校验执行资格：PENDING 转 RESOLVING；已是 RESOLVING（恢复路径）原样返回；否则 null */
    @Transactional(rollbackFor = Exception.class)
    public QtPageDeployment claimOrVerify(long deploymentId)
    {
        QtPageDeployment deployment = deploymentMapper.selectForUpdate(deploymentId);
        if (deployment == null)
        {
            return null;
        }
        if (QtPageDeployment.STATUS_PENDING.equals(deployment.getStatus()))
        {
            deployment.setStatus(QtPageDeployment.STATUS_RESOLVING);
            deploymentMapper.updateDeployment(deployment);
            return deployment;
        }
        return QtPageDeployment.STATUS_RESOLVING.equals(deployment.getStatus()) ? deployment : null;
    }

    // ---------- 执行流水线中的短事务 ----------

    @Transactional(rollbackFor = Exception.class)
    public boolean isStale(Long siteId, Long requestVersion, Long configVersion)
    {
        QtPageSite site = siteMapper.selectForUpdate(siteId);
        return site == null
                || !site.getRequestVersion().equals(requestVersion)
                || !site.getConfigVersion().equals(configVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markSuperseded(long deploymentId, String logText, Date now)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (current == null || isTerminal(current.getStatus()))
        {
            return;
        }
        current.setStatus(QtPageDeployment.STATUS_SUPERSEDED);
        current.setFinishedTime(now);
        current.setLogText(logText);
        deploymentMapper.updateDeployment(current);
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistCommitSha(long deploymentId, String sha)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (current == null || !QtPageDeployment.STATUS_RESOLVING.equals(current.getStatus()))
        {
            return;
        }
        current.setCommitSha(sha);
        deploymentMapper.updateDeployment(current);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transition(long deploymentId, String status, String logText)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (current == null || isTerminal(current.getStatus()))
        {
            return;
        }
        current.setStatus(status);
        current.setLogText(logText);
        deploymentMapper.updateDeployment(current);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reschedule(long deploymentId, Date nextAttempt)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (current == null || !QtPageDeployment.STATUS_RESOLVING.equals(current.getStatus()))
        {
            return;
        }
        current.setStatus(QtPageDeployment.STATUS_PENDING);
        current.setNextAttemptTime(nextAttempt);
        deploymentMapper.updateDeployment(current);
    }

    public record PublishingBoundary(boolean superseded, Long previousReleaseId)
    {
    }

    /**
     * 发布边界：行锁下复核意图仍最新后，持久化 PUBLISHING 状态与 previous_release_id。
     * 提交后即进入"文件切换可由恢复对账"的阶段。
     */
    @Transactional(rollbackFor = Exception.class)
    public PublishingBoundary persistPublishingBoundary(long deploymentId, long siteId, long releaseId,
            int fileCount, long totalBytes, String logText, Date now)
    {
        QtPageSite site = siteMapper.selectForUpdate(siteId);
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (site == null || current == null)
        {
            throw new IllegalStateException("站点或部署任务不存在");
        }
        if (!site.getRequestVersion().equals(current.getRequestVersion())
                || !site.getConfigVersion().equals(current.getConfigVersion()))
        {
            current.setStatus(QtPageDeployment.STATUS_SUPERSEDED);
            current.setFinishedTime(now);
            current.setLogText(logText);
            deploymentMapper.updateDeployment(current);
            return new PublishingBoundary(true, null);
        }
        current.setStatus(QtPageDeployment.STATUS_PUBLISHING);
        current.setReleaseId(releaseId);
        current.setPreviousReleaseId(site.getCurrentReleaseId());
        current.setFileCount(fileCount);
        current.setTotalBytes(totalBytes);
        current.setLogText(logText);
        deploymentMapper.updateDeployment(current);
        return new PublishingBoundary(false, site.getCurrentReleaseId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void finalizeSuccess(long deploymentId, long siteId, String sha, long releaseId, String logText, Date now)
    {
        QtPageSite site = siteMapper.selectForUpdate(siteId);
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (site == null || current == null)
        {
            return;
        }
        site.setCurrentSha(sha);
        site.setCurrentDeploymentId(deploymentId);
        site.setCurrentReleaseId(releaseId);
        siteMapper.updateSite(site);
        current.setStatus(QtPageDeployment.STATUS_SUCCESS);
        current.setFinishedTime(now);
        current.setReleaseAvailable(true);
        current.setLogText(logText);
        deploymentMapper.updateDeployment(current);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(long deploymentId, String errorCode, String errorMessage, String logText,
            Date now, boolean retryable, Instant retryAt, QtPagesProperties properties)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (current == null || isTerminal(current.getStatus()))
        {
            return;
        }
        int attempts = (current.getAttemptCount() == null ? 0 : current.getAttemptCount()) + 1;
        current.setAttemptCount(attempts);
        current.setErrorCode(errorCode);
        current.setErrorMessage(truncate(errorMessage, 1000));
        if (retryable && attempts < properties.getMaxAttempts())
        {
            current.setStatus(QtPageDeployment.STATUS_PENDING);
            List<Integer> backoffs = properties.getRetryBackoffSeconds();
            int backoffIndex = Math.min(attempts - 1, backoffs.size() - 1);
            Date next = new Date(now.getTime() + backoffs.get(backoffIndex) * 1000L);
            if (retryAt != null && Date.from(retryAt).after(next))
            {
                next = Date.from(retryAt);
            }
            current.setNextAttemptTime(next);
        }
        else
        {
            current.setStatus(QtPageDeployment.STATUS_FAILED);
            current.setFinishedTime(now);
            current.setReleaseAvailable(false);
            current.setNextAttemptTime(null);
        }
        current.setLogText(logText);
        deploymentMapper.updateDeployment(current);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markTerminalFailure(long deploymentId, String errorCode, String errorMessage, Date now)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (current == null || isTerminal(current.getStatus()))
        {
            return;
        }
        current.setStatus(QtPageDeployment.STATUS_FAILED);
        current.setFinishedTime(now);
        current.setErrorCode(errorCode);
        current.setErrorMessage(errorMessage);
        current.setReleaseAvailable(false);
        deploymentMapper.updateDeployment(current);
    }

    // ---------- 启动恢复 ----------

    /** 中断在 RESOLVING/DOWNLOADING/VALIDATING 的任务：有更高意图则废弃，预算内重排，否则终态失败 */
    @Transactional(rollbackFor = Exception.class)
    public void recoverInterruptedOne(long deploymentId, Date now, int maxAttempts)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        QtPageSite site = current == null ? null : siteMapper.selectForUpdate(current.getSiteId());
        if (current == null || site == null || !isRecoverable(current.getStatus()))
        {
            return;
        }
        if (site.getRequestVersion() > current.getRequestVersion()
                || site.getConfigVersion() > current.getConfigVersion())
        {
            current.setStatus(QtPageDeployment.STATUS_SUPERSEDED);
            current.setFinishedTime(now);
        }
        else if (current.getAttemptCount() != null && current.getAttemptCount() >= maxAttempts)
        {
            current.setStatus(QtPageDeployment.STATUS_FAILED);
            current.setFinishedTime(now);
            current.setErrorCode("INTERRUPTED");
            current.setErrorMessage("任务中断且尝试预算已用尽");
        }
        else
        {
            current.setStatus(QtPageDeployment.STATUS_PENDING);
            current.setNextAttemptTime(now);
        }
        deploymentMapper.updateDeployment(current);
    }

    /**
     * PUBLISHING 对账：磁盘真实 current 与数据库核对——已切换则补记成功；
     * 未切换按中断重排或失败；指向无法识别则暂停站点。绝不倒退更新的意图。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reconcileOne(long deploymentId, Date now, int maxAttempts)
    {
        QtPageDeployment current = deploymentMapper.selectForUpdate(deploymentId);
        if (current == null || !QtPageDeployment.STATUS_PUBLISHING.equals(current.getStatus()))
        {
            return;
        }
        QtPageSite site = siteMapper.selectForUpdate(current.getSiteId());
        if (site == null)
        {
            return;
        }
        QtPageReleaseStore store = runtime.store();
        Long actual;
        try
        {
            actual = store.readCurrentRelease(site);
        }
        catch (Exception e)
        {
            actual = null;
        }
        if (actual != null && actual.equals(current.getReleaseId()))
        {
            // 磁盘已切换、数据库未提交：补记成功，不重发布
            if (site.getRequestVersion().equals(current.getRequestVersion())
                    && site.getConfigVersion().equals(current.getConfigVersion()))
            {
                site.setCurrentSha(current.getCommitSha());
                site.setCurrentDeploymentId(current.getDeploymentId());
                site.setCurrentReleaseId(current.getReleaseId());
                siteMapper.updateSite(site);
                current.setStatus(QtPageDeployment.STATUS_SUCCESS);
                current.setFinishedTime(now);
                current.setReleaseAvailable(true);
            }
            else
            {
                current.setStatus(QtPageDeployment.STATUS_SUPERSEDED);
                current.setFinishedTime(now);
            }
        }
        else if (site.getRequestVersion() > current.getRequestVersion())
        {
            current.setStatus(QtPageDeployment.STATUS_SUPERSEDED);
            current.setFinishedTime(now);
        }
        else if ((actual != null && actual.equals(current.getPreviousReleaseId()))
                || (actual == null && site.getCurrentReleaseId() == null))
        {
            // 仍未切换：按中断处理，预算内重新排队
            if (current.getAttemptCount() != null && current.getAttemptCount() >= maxAttempts)
            {
                current.setStatus(QtPageDeployment.STATUS_FAILED);
                current.setFinishedTime(now);
                current.setErrorCode("PUBLISHING_UNFINISHED");
                current.setErrorMessage("发布中断且尝试预算已用尽，站点保持原版本");
            }
            else
            {
                current.setStatus(QtPageDeployment.STATUS_PENDING);
                current.setNextAttemptTime(now);
            }
        }
        else
        {
            site.setStatus(QtPageSite.STATUS_SUSPENDED);
            siteMapper.updateSite(site);
            current.setStatus(QtPageDeployment.STATUS_FAILED);
            current.setFinishedTime(now);
            current.setErrorCode("RECOVERY_UNKNOWN_CURRENT");
            current.setErrorMessage("检测到无法识别的发布状态，站点已暂停，请联系管理员");
        }
        deploymentMapper.updateDeployment(current);
    }

    private static QtPageDeployment newDeployment(QtPageSite site, String triggerType, long requestVersion)
    {
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
        return deployment;
    }

    private static boolean isTerminal(String status)
    {
        return QtPageDeployment.STATUS_SUCCESS.equals(status)
                || QtPageDeployment.STATUS_FAILED.equals(status)
                || QtPageDeployment.STATUS_SUPERSEDED.equals(status);
    }

    private static boolean isRecoverable(String status)
    {
        return QtPageDeployment.STATUS_RESOLVING.equals(status)
                || QtPageDeployment.STATUS_DOWNLOADING.equals(status)
                || QtPageDeployment.STATUS_VALIDATING.equals(status);
    }

    private static String truncate(String value, int limit)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
