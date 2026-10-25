package com.ruoyi.qt.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.config.QtPagesProperties;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtPageDeploymentMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtGitHubClient;
import com.ruoyi.qt.pages.QtPageReleaseStore;
import com.ruoyi.qt.pages.QtPagesAccess;
import com.ruoyi.qt.pages.QtPagesRuntime;
import com.ruoyi.qt.pages.QtStaticArchiveValidator;
import com.ruoyi.qt.service.IQtPageDeployService;

/**
 * 部署任务入队、轮询、运行与恢复的编排实现。
 *
 * 编排层不做数据库事务：全部短事务位于 QtPageDeployTransactions（经 Spring 代理），
 * 网络与文件工作都在事务外。发布采用两阶段协议——先在行锁下持久化 PUBLISHING 边界
 * （previous_release_id/release_id），再原子切换 current，切换后提交 SUCCESS；
 * 进程在任何一点退出都由 reconcilePublishing/recoverInterrupted 对账。
 */
@Service
public class QtPageDeployServiceImpl implements IQtPageDeployService
{
    private static final Logger log = LoggerFactory.getLogger(QtPageDeployServiceImpl.class);

    @Autowired
    private QtPageSiteMapper siteMapper;

    @Autowired
    private QtPageDeploymentMapper deploymentMapper;

    @Autowired
    private QtPagesAccess access;

    @Autowired
    private QtPagesRuntime runtime;

    @Autowired
    private QtPageDeployTransactions transactions;

    @Autowired(required = false)
    @Qualifier("qtPagesDeployExecutor")
    private ExecutorService deployExecutor;

    private Clock clock = Clock.systemUTC();

    void setClock(Clock clock)
    {
        this.clock = clock;
    }

    void setDeployExecutor(ExecutorService deployExecutor)
    {
        this.deployExecutor = deployExecutor;
    }

    @Override
    public QtPageDeployment requestDeploy(Long siteId, String triggerType)
    {
        runtime.requireReady();
        QtPageSite site = access.requireOwnedSite(siteId);
        return enqueueLocked(site, triggerType);
    }

    @Override
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

    @Override
    public void pollDueSites()
    {
        if (!runtime.isReady())
        {
            return;
        }
        Date now = Date.from(clock.instant());
        List<QtPageSite> due = siteMapper.selectDueSites(now, 50);
        for (QtPageSite site : due)
        {
            // selectDueSites 已按 ENABLED 过滤，这里防御暂停站点触发无谓的 API 调用
            if (!QtPageSite.STATUS_ENABLED.equals(site.getStatus()))
            {
                continue;
            }
            try
            {
                QtGitHubClient.Head head = runtime.gitHubClient().getBranchHead(
                        new QtGitHubClient.Source(site.getRepoOwner(), site.getRepoName(), site.getBranch()),
                        site.getPollEtag());
                Date nextPoll = plusSeconds(now, runtime.properties().getPollIntervalSeconds());
                transactions.applyPollHead(site.getSiteId(), head, now, nextPoll);
            }
            catch (QtGitHubClient.GitHubClientException e)
            {
                recordPollFailure(site, e.getErrorCode(), e.getRetryAt(), e.getMessage());
            }
            catch (Exception e)
            {
                log.error("Pages 轮询站点 {} 出现未预期异常", site.getSiteId(), e);
                recordPollFailure(site, QtGitHubClient.ERROR_NETWORK, null, e.getMessage());
            }
        }
    }

    private void recordPollFailure(QtPageSite site, String errorCode, Instant retryAt, String message)
    {
        Date now = Date.from(clock.instant());
        Date nextPoll = plusSeconds(now, runtime.properties().getPollIntervalSeconds());
        if (retryAt != null && Date.from(retryAt).after(nextPoll))
        {
            nextPoll = Date.from(retryAt);
        }
        try
        {
            transactions.recordPollFailure(site.getSiteId(), errorCode, message, now, nextPoll);
        }
        catch (Exception e)
        {
            log.error("Pages 轮询错误落库失败 site={}", site.getSiteId(), e);
        }
    }

    @Override
    public void dispatchPending()
    {
        if (!runtime.isReady() || deployExecutor == null)
        {
            return;
        }
        Date now = Date.from(clock.instant());
        int batchLimit = Math.max(1, runtime.properties().getDeployExecutorThreads()
                + runtime.properties().getExecutorQueueSize());
        List<QtPageDeployment> due = deploymentMapper.selectDispatchable(now, batchLimit);
        for (QtPageDeployment deployment : due)
        {
            long deploymentId = deployment.getDeploymentId();
            if (!transactions.claim(deploymentId))
            {
                continue;
            }
            try
            {
                deployExecutor.submit(() -> {
                    try
                    {
                        runDeployment(deploymentId);
                    }
                    catch (Exception e)
                    {
                        log.error("Pages 部署任务 {} 运行异常", deploymentId, e);
                    }
                });
            }
            catch (RejectedExecutionException e)
            {
                // 队列已满：任务回退为 PENDING，等待下一轮扫描，不丢失
                transactions.unclaim(deploymentId, Date.from(clock.instant()));
                break;
            }
        }
    }

    @Override
    public void runDeployment(long deploymentId)
    {
        runtime.requireReady();
        QtPagesProperties properties = runtime.properties();
        Date now = Date.from(clock.instant());
        long deadlineNanos = System.nanoTime() + Duration.ofSeconds(properties.getDeployTimeoutSeconds()).toNanos();
        QtPageDeployment deployment = transactions.claimOrVerify(deploymentId);
        if (deployment == null)
        {
            return;
        }
        QtPageSite site = siteMapper.selectById(deployment.getSiteId());
        if (site == null)
        {
            transactions.markTerminalFailure(deploymentId, "SITE_MISSING", "站点已不存在", now);
            return;
        }
        QtPageReleaseStore store = runtime.store();
        Optional<QtPageReleaseStore.SiteLock> locked = Optional.empty();
        LogBuilder logText = new LogBuilder(properties.getLogLimitBytes());
        try
        {
            locked = store.tryLock(site);
            if (!locked.isPresent())
            {
                transactions.reschedule(deploymentId, plusSeconds(now, 15));
                return;
            }
            runPipeline(deployment, site, store, properties, logText, deadlineNanos);
        }
        catch (QtGitHubClient.GitHubClientException e)
        {
            recordFailure(deployment, site, e.getErrorCode(), e.isRetryable(), e.getRetryAt(), e.getMessage(),
                    logText, properties);
        }
        catch (QtStaticArchiveValidator.ArchiveValidationException e)
        {
            recordFailure(deployment, site, e.getErrorCode(), false, null, e.getMessage(), logText, properties);
        }
        catch (QtPageReleaseStore.ReleaseMissingException e)
        {
            recordFailure(deployment, site, "RELEASE_MISSING", false, null, e.getMessage(), logText, properties);
        }
        catch (QtPageReleaseStore.QuotaExceededException e)
        {
            recordFailure(deployment, site, "DISK_QUOTA_EXCEEDED", false, null, e.getMessage(), logText, properties);
        }
        catch (IOException e)
        {
            recordFailure(deployment, site, QtGitHubClient.ERROR_NETWORK, true, null, e.getMessage(),
                    logText, properties);
        }
        catch (Exception e)
        {
            log.error("Pages 部署 {} 出现未预期异常", deploymentId, e);
            recordFailure(deployment, site, "INTERNAL_ERROR", false, null,
                    safeMessage(e), logText, properties);
        }
        finally
        {
            locked.ifPresent(QtPageReleaseStore.SiteLock::close);
        }
    }

    private void runPipeline(QtPageDeployment deployment, QtPageSite site, QtPageReleaseStore store,
            QtPagesProperties properties, LogBuilder logText, long deadlineNanos) throws IOException
    {
        // 1. 意图过时检查：目标已被更新意图取代时直接废弃，不触网、不改 current
        if (transactions.isStale(site.getSiteId(), deployment.getRequestVersion(), deployment.getConfigVersion()))
        {
            logText.append("superseded before resolution");
            transactions.markSuperseded(deployment.getDeploymentId(), logText.toString(),
                    Date.from(clock.instant()));
            cleanupStaging(store, site, deployment.getDeploymentId());
            return;
        }
        // 回滚不下载：直接引用来源实体目录，走同一套两阶段发布与恢复协议
        if (QtPageDeployment.TRIGGER_ROLLBACK.equals(deployment.getTriggerType()))
        {
            runRollbackPipeline(deployment, site, store, logText);
            return;
        }
        QtGitHubClient client = runtime.gitHubClient();
        QtGitHubClient.Source source = new QtGitHubClient.Source(deployment.getRepoOwner(),
                deployment.getRepoName(), deployment.getBranch());

        // 2. 磁盘预算：先回收不受保护的过期版本，配额仍不足则拒绝新部署（不动在线版本）
        enforceDiskBudget(store, site, properties, logText);

        // 3. RESOLVING：确认公开仓库并固定 SHA
        client.requirePublicRepository(source);
        String sha = deployment.getCommitSha();
        if (sha == null)
        {
            QtGitHubClient.Head head = client.getBranchHead(source, null);
            sha = head.sha();
            transactions.persistCommitSha(deployment.getDeploymentId(), sha);
        }
        logText.append("resolved commit " + sha);

        // 3. DOWNLOADING：受限流式下载。归档放在 staging 目录之外：
        //    validator.extract 会先清空 staging，归档若在其中会被自己删掉
        transactions.transition(deployment.getDeploymentId(), QtPageDeployment.STATUS_DOWNLOADING,
                logText.toString());
        Path staging = store.stagingDirectory(site, deployment.getDeploymentId());
        Files.createDirectories(staging);
        Path archive = staging.getParent().resolve(deployment.getDeploymentId() + "-archive.tar.gz");
        client.downloadArchive(source, sha, archive);

        // 4. VALIDATING：安全解包与预算校验
        if (System.nanoTime() > deadlineNanos)
        {
            throw new QtGitHubClient.GitHubClientException(QtGitHubClient.ERROR_TIMEOUT, false,
                    "deploy exceeded time limit before validation");
        }
        transactions.transition(deployment.getDeploymentId(), QtPageDeployment.STATUS_VALIDATING,
                logText.toString());
        QtStaticArchiveValidator.ValidatedArtifact artifact =
                runtime.validator().extract(archive, deployment.getPublishDir(), staging);
        Files.deleteIfExists(archive);

        // 5. 固化为实体 release（普通部署 release_id = 自身 deployment_id）
        long releaseId = deployment.getReleaseId() != null ? deployment.getReleaseId() : deployment.getDeploymentId();
        store.prepare(site, releaseId, artifact);
        logText.append("prepared release " + releaseId);

        // 6. PUBLISHING 阶段一：行锁下持久化发布边界，此后进程退出也可由恢复协议对账
        Date boundaryAt = Date.from(clock.instant());
        QtPageDeployTransactions.PublishingBoundary boundary = transactions.persistPublishingBoundary(
                deployment.getDeploymentId(), site.getSiteId(), releaseId,
                artifact.fileCount(), artifact.totalBytes(), logText.toString(), boundaryAt);
        if (boundary.superseded())
        {
            logText.append("superseded before publish");
            cleanupStaging(store, site, deployment.getDeploymentId());
            return;
        }

        // 7. 原子切换 current（持有站点文件锁）
        if (System.nanoTime() > deadlineNanos)
        {
            throw new QtGitHubClient.GitHubClientException(QtGitHubClient.ERROR_TIMEOUT, false,
                    "deploy exceeded time limit before publish");
        }
        store.switchCurrent(site, releaseId);

        // 8. PUBLISHING 阶段二：提交在线状态，随后回收过期版本
        transactions.finalizeSuccess(deployment.getDeploymentId(), site.getSiteId(), sha, releaseId,
                logText.append("published").toString(), Date.from(clock.instant()));
        pruneExpiredReleases(store, site);
    }

    private void runRollbackPipeline(QtPageDeployment deployment, QtPageSite site, QtPageReleaseStore store,
            LogBuilder logText) throws IOException
    {
        long releaseId = deployment.getReleaseId() != null ? deployment.getReleaseId() : deployment.getDeploymentId();
        if (!Files.isDirectory(store.releaseDirectory(site, releaseId)))
        {
            throw new QtPageReleaseStore.ReleaseMissingException("rollback target release missing: " + releaseId);
        }
        logText.append("rollback to release " + releaseId);
        QtPageDeployTransactions.PublishingBoundary boundary = transactions.persistPublishingBoundary(
                deployment.getDeploymentId(), site.getSiteId(), releaseId,
                deployment.getFileCount() == null ? 0 : deployment.getFileCount(),
                deployment.getTotalBytes() == null ? 0L : deployment.getTotalBytes(),
                logText.toString(), Date.from(clock.instant()));
        if (boundary.superseded())
        {
            logText.append("superseded before publish");
            return;
        }
        store.switchCurrent(site, releaseId);
        transactions.finalizeSuccess(deployment.getDeploymentId(), site.getSiteId(), deployment.getCommitSha(),
                releaseId, logText.append("published").toString(), Date.from(clock.instant()));
        pruneExpiredReleases(store, site);
    }

    private void enforceDiskBudget(QtPageReleaseStore store, QtPageSite site, QtPagesProperties properties,
            LogBuilder logText)
    {
        pruneExpiredReleases(store, site);
        long usage;
        try
        {
            usage = store.siteUsageBytes(site);
        }
        catch (IOException e)
        {
            usage = 0L;
        }
        if (usage + properties.getDownloadLimitBytes() > properties.getDiskQuotaBytes())
        {
            throw new QtPageReleaseStore.QuotaExceededException(
                    "站点磁盘占用已达上限，请清理不再需要的版本后重试");
        }
        logText.append("disk usage " + usage + " bytes");
    }

    private void pruneExpiredReleases(QtPageReleaseStore store, QtPageSite site)
    {
        try
        {
            for (Long releaseId : transactions.planPrune(site.getSiteId()).deletableReleaseIds())
            {
                try
                {
                    QtStaticArchiveValidator.deleteRecursively(store.releaseDirectory(site, releaseId));
                }
                catch (IOException e)
                {
                    log.warn("Pages release {} 删除失败", releaseId, e);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Pages 版本回收跳过 site={}", site.getSiteId(), e);
        }
    }

    private void recordFailure(QtPageDeployment deployment, QtPageSite site, String errorCode, boolean retryable,
            Instant retryAt, String message, LogBuilder logText, QtPagesProperties properties)
    {
        log.warn("Pages 部署 {} 失败：{}", deployment.getDeploymentId(), errorCode);
        logText.append("failed " + errorCode);
        try
        {
            transactions.recordFailure(deployment.getDeploymentId(), errorCode, safeMessage(message),
                    logText.toString(), Date.from(clock.instant()), retryable, retryAt, properties);
        }
        catch (Exception e)
        {
            log.error("Pages 部署失败状态落库失败 deployment={}", deployment.getDeploymentId(), e);
        }
        cleanupStaging(runtime.store(), site, deployment.getDeploymentId());
    }

    @Override
    public void recoverInterrupted()
    {
        Date now = Date.from(clock.instant());
        int maxAttempts = runtime.properties().getMaxAttempts();
        for (QtPageDeployment stuck : deploymentMapper.selectInterrupted())
        {
            QtPageSite site = siteMapper.selectById(stuck.getSiteId());
            if (site == null)
            {
                continue;
            }
            Optional<QtPageReleaseStore.SiteLock> locked = Optional.empty();
            try
            {
                locked = runtime.store().tryLock(site);
                if (!locked.isPresent())
                {
                    continue;
                }
                transactions.recoverInterruptedOne(stuck.getDeploymentId(), now, maxAttempts);
            }
            catch (Exception e)
            {
                log.error("Pages 中断任务恢复失败 deployment={}", stuck.getDeploymentId(), e);
            }
            finally
            {
                locked.ifPresent(QtPageReleaseStore.SiteLock::close);
            }
        }
    }

    @Override
    public void reconcilePublishing()
    {
        Date now = Date.from(clock.instant());
        int maxAttempts = runtime.properties().getMaxAttempts();
        for (QtPageDeployment stuck : deploymentMapper.selectPublishing())
        {
            QtPageSite site = siteMapper.selectById(stuck.getSiteId());
            if (site == null)
            {
                continue;
            }
            Optional<QtPageReleaseStore.SiteLock> locked = Optional.empty();
            try
            {
                locked = runtime.store().tryLock(site);
                if (!locked.isPresent())
                {
                    log.warn("Pages 恢复跳过站点 {}：文件锁被占用", site.getSiteId());
                    continue;
                }
                transactions.reconcileOne(stuck.getDeploymentId(), now, maxAttempts);
            }
            catch (Exception e)
            {
                log.error("Pages PUBLISHING 对账失败 deployment={}", stuck.getDeploymentId(), e);
            }
            finally
            {
                locked.ifPresent(QtPageReleaseStore.SiteLock::close);
            }
        }
    }

    @Override
    public java.util.List<QtPageDeployment> listOwnedDeployments(Long siteId, Integer pageNum, Integer pageSize)
    {
        access.requireOwnedSite(siteId);
        int number = pageNum == null || pageNum < 1 ? 1 : pageNum;
        com.github.pagehelper.PageHelper.startPage(number, normalizePageSize(pageSize));
        return deploymentMapper.selectListBySite(siteId);
    }

    @Override
    public QtPageDeployment getOwnedDeployment(Long siteId, Long deploymentId)
    {
        access.requireOwnedSite(siteId);
        QtPageDeployment deployment = deploymentId == null ? null : deploymentMapper.selectById(deploymentId);
        if (deployment == null || !siteId.equals(deployment.getSiteId()))
        {
            throw new ServiceException("部署记录不存在", HttpStatus.NOT_FOUND);
        }
        return deployment;
    }

    @Override
    public QtPageDeployment requestRollback(Long siteId, Long deploymentId)
    {
        runtime.requireReady();
        access.requireOwnedSite(siteId);
        return transactions.enqueueRollback(siteId, deploymentId, Date.from(clock.instant()));
    }

    /** 历史分页：默认 20，最大 100。 */
    static int normalizePageSize(Integer pageSize)
    {
        if (pageSize == null || pageSize < 1)
        {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private void cleanupStaging(QtPageReleaseStore store, QtPageSite site, long deploymentId)
    {
        try
        {
            QtStaticArchiveValidator.deleteRecursively(store.stagingDirectory(site, deploymentId));
            QtStaticArchiveValidator.deleteRecursively(
                    store.stagingRoot(site).resolve(deploymentId + "-archive.tar.gz"));
        }
        catch (IOException ignored)
        {
        }
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

    private static Date plusSeconds(Date base, long seconds)
    {
        return new Date(base.getTime() + Duration.ofSeconds(seconds).toMillis());
    }

    /** 面向成员的安全错误摘要：只保留第一行，剥离异常类名与服务器细节。 */
    private static String safeMessage(Throwable error)
    {
        String message = error.getMessage();
        if (StringUtils.isEmpty(message))
        {
            return error.getClass().getSimpleName();
        }
        return firstLine(message);
    }

    private static String safeMessage(String message)
    {
        if (StringUtils.isEmpty(message))
        {
            return "未知错误";
        }
        return firstLine(message);
    }

    private static String firstLine(String message)
    {
        int newline = message.indexOf('\n');
        String firstLine = newline >= 0 ? message.substring(0, newline) : message;
        return firstLine.length() > 300 ? firstLine.substring(0, 300) : firstLine;
    }

    /** 阶段日志：总量不超过上限，超限保留最新内容。 */
    static final class LogBuilder
    {
        private final StringBuilder builder = new StringBuilder();

        private final int limit;

        LogBuilder(int limit)
        {
            this.limit = limit;
        }

        LogBuilder append(String line)
        {
            builder.append(line).append('\n');
            if (builder.length() > limit)
            {
                builder.delete(0, builder.length() - limit);
            }
            return this;
        }

        @Override
        public String toString()
        {
            return builder.toString();
        }
    }
}
