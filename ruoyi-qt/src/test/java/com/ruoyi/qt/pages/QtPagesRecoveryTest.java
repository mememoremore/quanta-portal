package com.ruoyi.qt.pages;

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
import com.ruoyi.qt.config.QtPagesProperties;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtPageDeploymentMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.service.impl.QtPageDeployServiceImpl;
import com.ruoyi.qt.service.impl.QtPageDeployTransactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * PUBLISHING 两阶段协议的崩溃恢复对账。涉及真实 current 符号链接的用例仅在 Linux 运行；
 * 纯数据库对账用例在所有平台运行。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QtPagesRecoveryTest
{
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @TempDir
    Path tempDir;

    @Mock
    private QtPageSiteMapper siteMapper;

    @Mock
    private QtPageDeploymentMapper deploymentMapper;

    @Mock
    private QtPagesRuntime runtime;

    private QtPageReleaseStore store;

    private QtPageSite site;

    @BeforeEach
    void setUp()
    {
        QtPagesProperties properties = new QtPagesProperties();
        store = new QtPageReleaseStore(tempDir);
        when(runtime.properties()).thenReturn(properties);
        when(runtime.store()).thenReturn(store);

        site = new QtPageSite();
        site.setSiteId(9L);
        site.setSlug("recover-lab");
        site.setRepoOwner("octo");
        site.setRepoName("hello");
        site.setBranch("pages");
        site.setPublishDir("/");
        site.setConfigVersion(1L);
        site.setRequestVersion(2L);
        site.setStatus(QtPageSite.STATUS_ENABLED);
    }

    private QtPageDeployServiceImpl newService()
    {
        QtPageDeployServiceImpl service = new QtPageDeployServiceImpl();
        ReflectionTestUtils.setField(service, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(service, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(service, "runtime", runtime);
        QtPageDeployTransactions transactions = new QtPageDeployTransactions();
        ReflectionTestUtils.setField(transactions, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(transactions, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(transactions, "runtime", runtime);
        ReflectionTestUtils.setField(service, "transactions", transactions);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
        return service;
    }

    private QtPageDeployment publishing(long deploymentId, long requestVersion, Long releaseId, Long previousReleaseId)
    {
        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setDeploymentId(deploymentId);
        deployment.setSiteId(9L);
        deployment.setRequestVersion(requestVersion);
        deployment.setConfigVersion(1L);
        deployment.setRepoOwner("octo");
        deployment.setRepoName("hello");
        deployment.setBranch("pages");
        deployment.setPublishDir("/");
        deployment.setCommitSha("c".repeat(40));
        deployment.setTriggerType(QtPageDeployment.TRIGGER_MANUAL);
        deployment.setStatus(QtPageDeployment.STATUS_PUBLISHING);
        deployment.setReleaseId(releaseId);
        deployment.setPreviousReleaseId(previousReleaseId);
        deployment.setAttemptCount(1);
        return deployment;
    }

    @Test
    void unswitchedPublishingRequeuesWithinBudget()
    {
        // 进程在持久化 PUBLISHING 边界后、切换 current 前退出：站点本来就没有在线版本
        QtPageDeployment deployment = publishing(30L, 2L, 30L, null);
        when(deploymentMapper.selectPublishing()).thenReturn(java.util.List.of(deployment));
        when(deploymentMapper.selectForUpdate(30L)).thenReturn(deployment);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);

        newService().reconcilePublishing();

        assertEquals(QtPageDeployment.STATUS_PENDING, deployment.getStatus());
        assertEquals(NOW.toEpochMilli(), deployment.getNextAttemptTime().getTime());
        assertNull(site.getCurrentReleaseId());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void switchedPublishingFinalizesSuccess()
    throws Exception
    {
        // 进程在切换 current 后、提交 SUCCESS 前退出：磁盘已指向 release 30，需补记成功
        Path release = store.releaseDirectory(site, 30L);
        Files.createDirectories(release);
        Files.writeString(release.resolve("index.html"), "v30");
        store.switchCurrent(site, 30L);
        QtPageDeployment deployment = publishing(30L, 2L, 30L, null);
        when(deploymentMapper.selectPublishing()).thenReturn(java.util.List.of(deployment));
        when(deploymentMapper.selectForUpdate(30L)).thenReturn(deployment);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);

        newService().reconcilePublishing();

        assertEquals(QtPageDeployment.STATUS_SUCCESS, deployment.getStatus());
        assertTrue(deployment.getReleaseAvailable());
        assertEquals(Long.valueOf(30L), site.getCurrentReleaseId());
        assertEquals(deployment.getCommitSha(), site.getCurrentSha());
        assertEquals(deployment.getCommitSha(), site.getCurrentSha());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void unrecognizedCurrentSuspendsSite()
    throws Exception
    {
        // current 指向平台无法解释的目标：暂停站点并报恢复错误，不伪报成功
        Path unknown = store.releaseDirectory(site, 999L);
        Files.createDirectories(unknown);
        Files.writeString(unknown.resolve("index.html"), "foreign");
        store.switchCurrent(site, 999L);
        QtPageDeployment deployment = publishing(30L, 2L, 30L, null);
        when(deploymentMapper.selectPublishing()).thenReturn(java.util.List.of(deployment));
        when(deploymentMapper.selectForUpdate(30L)).thenReturn(deployment);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);

        newService().reconcilePublishing();

        assertEquals(QtPageDeployment.STATUS_FAILED, deployment.getStatus());
        assertEquals("RECOVERY_UNKNOWN_CURRENT", deployment.getErrorCode());
        assertEquals(QtPageSite.STATUS_SUSPENDED, site.getStatus());
        assertNull(site.getCurrentReleaseId());
    }

    @Test
    void recoveryNeverRollsBackToOlderRequestVersion()
    {
        // 新意图已把站点推进到 request_version=5：遗留的旧 PUBLISHING 任务直接废弃
        site.setRequestVersion(5L);
        QtPageDeployment deployment = publishing(30L, 2L, 30L, null);
        when(deploymentMapper.selectPublishing()).thenReturn(java.util.List.of(deployment));
        when(deploymentMapper.selectForUpdate(30L)).thenReturn(deployment);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);

        newService().reconcilePublishing();

        assertEquals(QtPageDeployment.STATUS_SUPERSEDED, deployment.getStatus());
        assertNull(site.getCurrentReleaseId());
        assertEquals(Long.valueOf(5L), site.getRequestVersion());
    }

    @Test
    void interruptedTasksRequeueThenFailWithinBudget()
    {
        QtPageDeployment interrupted = publishing(40L, 2L, 40L, null);
        interrupted.setStatus(QtPageDeployment.STATUS_DOWNLOADING);
        interrupted.setAttemptCount(1);
        when(deploymentMapper.selectInterrupted()).thenReturn(java.util.List.of(interrupted));
        when(deploymentMapper.selectForUpdate(40L)).thenReturn(interrupted);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);

        newService().recoverInterrupted();

        assertEquals(QtPageDeployment.STATUS_PENDING, interrupted.getStatus());
        assertEquals(NOW.toEpochMilli(), interrupted.getNextAttemptTime().getTime());

        interrupted.setStatus(QtPageDeployment.STATUS_VALIDATING);
        interrupted.setAttemptCount(3);
        newService().recoverInterrupted();
        assertEquals(QtPageDeployment.STATUS_FAILED, interrupted.getStatus());
        assertEquals("INTERRUPTED", interrupted.getErrorCode());
    }

    @Test
    void interruptedTaskWithNewerIntentIsSuperseded()
    {
        site.setRequestVersion(6L);
        QtPageDeployment interrupted = publishing(40L, 2L, 40L, null);
        interrupted.setStatus(QtPageDeployment.STATUS_RESOLVING);
        when(deploymentMapper.selectInterrupted()).thenReturn(java.util.List.of(interrupted));
        when(deploymentMapper.selectForUpdate(40L)).thenReturn(interrupted);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);

        newService().recoverInterrupted();

        assertEquals(QtPageDeployment.STATUS_SUPERSEDED, interrupted.getStatus());
        assertNull(interrupted.getNextAttemptTime());
    }
}
