package com.ruoyi.qt.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.config.QtPagesProperties;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtPageDeploymentMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtGitHubClient;
import com.ruoyi.qt.pages.QtPageReleaseStore;
import com.ruoyi.qt.pages.QtPagesAccess;
import com.ruoyi.qt.pages.QtPagesRuntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QtPageRollbackTest
{
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @TempDir
    Path tempDir;

    @Mock
    private QtPageSiteMapper siteMapper;

    @Mock
    private QtPageDeploymentMapper deploymentMapper;

    @Mock
    private QtPagesAccess access;

    @Mock
    private QtPagesRuntime runtime;

    private QtPageSite site;

    @BeforeEach
    void setUp()
    {
        QtPagesProperties properties = new QtPagesProperties();
        when(runtime.isReady()).thenReturn(true);
        when(runtime.properties()).thenReturn(properties);
        when(runtime.store()).thenReturn(new QtPageReleaseStore(tempDir));

        site = new QtPageSite();
        site.setSiteId(9L);
        site.setUserId(7L);
        site.setSlug("rollback-lab");
        site.setRepoOwner("octo");
        site.setRepoName("hello");
        site.setBranch("pages");
        site.setPublishDir("/");
        site.setConfigVersion(1L);
        site.setRequestVersion(5L);
        site.setStatus(QtPageSite.STATUS_ENABLED);
        site.setLastObservedSha("d".repeat(40));
    }

    /** 供磁盘预算测试对齐意图版本：站点推进到 rv=6 后再发起部署。 */
    private void advanceSiteToRequestVersion(long requestVersion)
    {
        site.setRequestVersion(requestVersion);
    }

    private QtPageDeployServiceImpl newService()
    {
        QtPageDeployServiceImpl service = new QtPageDeployServiceImpl();
        ReflectionTestUtils.setField(service, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(service, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(service, "access", access);
        ReflectionTestUtils.setField(service, "runtime", runtime);
        QtPageDeployTransactions transactions = new QtPageDeployTransactions();
        ReflectionTestUtils.setField(transactions, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(transactions, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(transactions, "runtime", runtime);
        ReflectionTestUtils.setField(service, "transactions", transactions);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
        return service;
    }

    private QtPageDeployment successDeployment(long deploymentId, long releaseId)
    {
        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setDeploymentId(deploymentId);
        deployment.setSiteId(9L);
        deployment.setRequestVersion(2L);
        deployment.setConfigVersion(1L);
        deployment.setRepoOwner("octo");
        deployment.setRepoName("hello");
        deployment.setBranch("pages");
        deployment.setPublishDir("/");
        deployment.setCommitSha("c".repeat(40));
        deployment.setTriggerType(QtPageDeployment.TRIGGER_POLL);
        deployment.setStatus(QtPageDeployment.STATUS_SUCCESS);
        deployment.setReleaseId(releaseId);
        deployment.setReleaseAvailable(true);
        deployment.setFileCount(3);
        deployment.setTotalBytes(300L);
        return deployment;
    }

    @Test
    void rollbackEnqueuesIndependentIntentWithoutDownload()
    {
        when(access.requireOwnedSite(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment source = successDeployment(30L, 30L);
        when(deploymentMapper.selectById(30L)).thenReturn(source);
        when(deploymentMapper.selectReusableRollback(9L)).thenReturn(null);
        doInsertAssignsId(40L);

        QtPageDeployment rollback = newService().requestRollback(9L, 30L);

        assertEquals(QtPageDeployment.TRIGGER_ROLLBACK, rollback.getTriggerType());
        assertEquals(Long.valueOf(30L), rollback.getReleaseId());
        assertEquals(source.getCommitSha(), rollback.getCommitSha());
        assertEquals(Long.valueOf(6L), rollback.getRequestVersion());
        assertEquals(Long.valueOf(6L), site.getRequestVersion());
        // 回滚不改变轮询观察
        assertEquals("d".repeat(40), site.getLastObservedSha());
        verify(deploymentMapper).markSupersededBefore(9L, 6L);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void rollbackSwitchesCurrentWithoutDownloadAndKeepsObservation()
    throws Exception
    {
        QtPageReleaseStore store = new QtPageReleaseStore(tempDir);
        Path release = store.releaseDirectory(site, 30L);
        Files.createDirectories(release);
        Files.writeString(release.resolve("index.html"), "old");
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment rollback = new QtPageDeployment();
        rollback.setDeploymentId(40L);
        rollback.setSiteId(9L);
        rollback.setRequestVersion(6L);
        rollback.setConfigVersion(1L);
        rollback.setRepoOwner("octo");
        rollback.setRepoName("hello");
        rollback.setBranch("pages");
        rollback.setPublishDir("/");
        rollback.setTriggerType(QtPageDeployment.TRIGGER_ROLLBACK);
        rollback.setStatus(QtPageDeployment.STATUS_PENDING);
        rollback.setReleaseId(30L);
        rollback.setCommitSha("c".repeat(40));
        rollback.setAttemptCount(0);
        when(deploymentMapper.selectForUpdate(40L)).thenReturn(rollback);

        newService().runDeployment(40L);

        assertEquals(QtPageDeployment.STATUS_SUCCESS, rollback.getStatus());
        assertEquals(Long.valueOf(30L), site.getCurrentReleaseId());
        assertEquals("c".repeat(40), site.getCurrentSha());
        assertEquals("d".repeat(40), site.getLastObservedSha());
        assertEquals(Long.valueOf(30L), store.readCurrentRelease(site));
        verify(runtime, never()).gitHubClient();
    }

    @Test
    void rejectsForeignFailedOrPrunedDeployment()
    {
        when(access.requireOwnedSite(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployServiceImpl service = newService();

        // 非本站记录 → 404
        QtPageDeployment foreign = successDeployment(31L, 31L);
        foreign.setSiteId(8L);
        when(deploymentMapper.selectById(31L)).thenReturn(foreign);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ServiceException.class, () -> service.requestRollback(9L, 31L)).getCode());

        // 失败记录 → 409
        QtPageDeployment failed = successDeployment(32L, 32L);
        failed.setStatus(QtPageDeployment.STATUS_FAILED);
        when(deploymentMapper.selectById(32L)).thenReturn(failed);
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ServiceException.class, () -> service.requestRollback(9L, 32L)).getCode());

        // 实体已回收 → 409
        QtPageDeployment pruned = successDeployment(33L, 33L);
        pruned.setReleaseAvailable(false);
        when(deploymentMapper.selectById(33L)).thenReturn(pruned);
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ServiceException.class, () -> service.requestRollback(9L, 33L)).getCode());
    }

    @Test
    void rollbackToCurrentVersionIsRejected()
    {
        when(access.requireOwnedSite(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        site.setCurrentReleaseId(30L);
        QtPageDeployment source = successDeployment(30L, 30L);
        when(deploymentMapper.selectById(30L)).thenReturn(source);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> newService().requestRollback(9L, 30L));
        assertEquals(HttpStatus.CONFLICT, exception.getCode());
    }

    @Test
    void repeatedRollbackToSameTargetReturnsQueuedTask()
    {
        when(access.requireOwnedSite(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment source = successDeployment(30L, 30L);
        when(deploymentMapper.selectById(30L)).thenReturn(source);
        QtPageDeployment queued = new QtPageDeployment();
        queued.setReleaseId(30L);
        queued.setStatus(QtPageDeployment.STATUS_PENDING);
        when(deploymentMapper.selectReusableRollback(9L)).thenReturn(queued);

        assertEquals(queued, newService().requestRollback(9L, 30L));
        verify(siteMapper, never()).updateSite(any());
    }

    @Test
    void queuedRollbackTargetIsProtectedFromPrune()
    throws Exception
    {
        // 排队中的 ROLLBACK 引用 release 10：回收时必须保留（保护集合由同一行锁下计算）
        QtPageReleaseStore store = new QtPageReleaseStore(tempDir);
        Files.createDirectories(store.releaseDirectory(site, 10L));
        Files.createDirectories(store.releaseDirectory(site, 11L));
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        when(deploymentMapper.selectProtectedReleaseIds(9L)).thenReturn(java.util.List.of(10L));
        when(runtime.store()).thenReturn(store);
        when(deploymentMapper.markReleaseUnavailable(9L, 11L)).thenReturn(1);

        java.util.List<Long> deletable = newTransactions().planPrune(9L).deletableReleaseIds();

        assertEquals(java.util.List.of(11L), deletable);
        verify(deploymentMapper).markReleaseUnavailable(9L, 11L);
        verify(deploymentMapper, never()).markReleaseUnavailable(9L, 10L);
    }

    @Test
    void diskBudgetRejectsWithoutRemovingCurrent()
    throws Exception
    {
        QtPagesProperties tight = new QtPagesProperties();
        tight.setDiskQuotaBytes(1000L);
        when(runtime.properties()).thenReturn(tight);
        when(runtime.isReady()).thenReturn(true);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        when(deploymentMapper.selectProtectedReleaseIds(9L)).thenReturn(java.util.List.of());
        QtPageReleaseStore store = new QtPageReleaseStore(tempDir);
        when(runtime.store()).thenReturn(store);

        // 当前在线版本 5 体积超过配额：新部署被拒绝，在线版本保持
        advanceSiteToRequestVersion(6L);
        Path current = store.releaseDirectory(site, 5L);
        Files.createDirectories(current);
        Files.writeString(current.resolve("index.html"), "x".repeat(4000));
        site.setCurrentReleaseId(5L);

        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setDeploymentId(50L);
        deployment.setSiteId(9L);
        deployment.setRequestVersion(6L);
        deployment.setConfigVersion(1L);
        deployment.setRepoOwner("octo");
        deployment.setRepoName("hello");
        deployment.setBranch("pages");
        deployment.setPublishDir("/");
        deployment.setTriggerType(QtPageDeployment.TRIGGER_MANUAL);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        deployment.setAttemptCount(0);
        when(deploymentMapper.selectForUpdate(50L)).thenReturn(deployment);

        newService().runDeployment(50L);

        assertEquals(QtPageDeployment.STATUS_FAILED, deployment.getStatus());
        assertEquals("DISK_QUOTA_EXCEEDED", deployment.getErrorCode());
        assertEquals(Long.valueOf(5L), site.getCurrentReleaseId());
        assertTrue(Files.exists(current), "quota rejection must not remove the current release");
    }

    @Test
    void pageSizeDefaultsTo20AndCapsAt100()
    {
        assertEquals(20, QtPageDeployServiceImpl.normalizePageSize(null));
        assertEquals(20, QtPageDeployServiceImpl.normalizePageSize(0));
        assertEquals(50, QtPageDeployServiceImpl.normalizePageSize(50));
        assertEquals(100, QtPageDeployServiceImpl.normalizePageSize(500));
    }

    @Test
    void logBuilderKeepsTailWithinLimit()
    {
        QtPageDeployServiceImpl.LogBuilder builder = new QtPageDeployServiceImpl.LogBuilder(64);
        for (int i = 0; i < 20; i++)
        {
            builder.append("line-" + i + "-padding-padding");
        }
        String text = builder.toString();
        assertTrue(text.length() <= 64);
        assertTrue(text.contains("line-19"));
    }

    private void doInsertAssignsId(long id)
    {
        org.mockito.Mockito.doAnswer(invocation -> {
            QtPageDeployment deployment = invocation.getArgument(0);
            deployment.setDeploymentId(id);
            return 1;
        }).when(deploymentMapper).insertDeployment(any());
    }

    private QtPageDeployTransactions newTransactions()
    {
        QtPageDeployTransactions transactions = new QtPageDeployTransactions();
        ReflectionTestUtils.setField(transactions, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(transactions, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(transactions, "runtime", runtime);
        return transactions;
    }
}
