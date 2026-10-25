package com.ruoyi.qt.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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
import com.ruoyi.qt.pages.QtStaticArchiveValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QtPageDeployServiceTest
{
    private static final String SHA_A = "a".repeat(40);

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

    @Mock
    private QtGitHubClient gitHubClient;

    private final Instant startTime = Instant.parse("2026-01-01T00:00:00Z");

    private QtPageDeployServiceImpl service;

    @BeforeEach
    void setUp()
    {
        QtPagesProperties properties = new QtPagesProperties();
        when(runtime.isReady()).thenReturn(true);
        when(runtime.properties()).thenReturn(properties);
        when(runtime.store()).thenReturn(new QtPageReleaseStore(tempDir));
        when(runtime.gitHubClient()).thenReturn(gitHubClient);
        when(runtime.validator()).thenReturn(new QtStaticArchiveValidator(1_000_000, 100_000, 100, 200));

        service = new QtPageDeployServiceImpl();
        ReflectionTestUtils.setField(service, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(service, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(service, "access", access);
        ReflectionTestUtils.setField(service, "runtime", runtime);
        ReflectionTestUtils.setField(service, "transactions", newTransactions(properties));
        service.setClock(Clock.fixed(startTime, ZoneOffset.UTC));
    }

    private QtPageDeployTransactions newTransactions(QtPagesProperties properties)
    {
        QtPageDeployTransactions transactions = new QtPageDeployTransactions();
        ReflectionTestUtils.setField(transactions, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(transactions, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(transactions, "runtime", runtime);
        return transactions;
    }

    @Test
    void requestDeployRequiresOwnedSite()
    {
        when(access.requireOwnedSite(99L)).thenThrow(new ServiceException("站点不存在", HttpStatus.NOT_FOUND));
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.requestDeploy(99L, QtPageDeployment.TRIGGER_MANUAL));
        assertEquals(HttpStatus.NOT_FOUND, exception.getCode());
        verify(siteMapper, never()).selectForUpdate(any());
    }

    @Test
    void sameCommitCanGetDifferentRequestVersionsOnEachManualIntent()
    {
        QtPageSite site = site(9L, 7L, 1L, 1L);
        when(access.requireOwnedSite(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        doAnswer(invocation -> {
            QtPageDeployment deployment = invocation.getArgument(0);
            deployment.setDeploymentId(deployment.getRequestVersion() * 10);
            return 1;
        }).when(deploymentMapper).insertDeployment(any());
        when(deploymentMapper.selectReusableManual(9L)).thenReturn(null);

        QtPageDeployment first = service.requestDeploy(9L, QtPageDeployment.TRIGGER_MANUAL);
        QtPageDeployment second = service.requestDeploy(9L, QtPageDeployment.TRIGGER_MANUAL);

        assertEquals(2L, first.getRequestVersion());
        assertEquals(3L, second.getRequestVersion());
        assertEquals(first.getReleaseId(), first.getDeploymentId());
        assertEquals(second.getReleaseId(), second.getDeploymentId());
        assertEquals(QtPageDeployment.STATUS_PENDING, first.getStatus());
        verify(siteMapper, times(2)).updateSite(site);
    }

    @Test
    void repeatedManualWithSameQueuedConfigReturnsExistingTask()
    {
        QtPageSite site = site(9L, 7L, 1L, 1L);
        when(access.requireOwnedSite(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment pending = new QtPageDeployment();
        pending.setSiteId(9L);
        pending.setRepoOwner("octo");
        pending.setRepoName("hello");
        pending.setBranch("pages");
        pending.setPublishDir("/");
        pending.setStatus(QtPageDeployment.STATUS_PENDING);
        when(deploymentMapper.selectReusableManual(9L)).thenReturn(pending);

        QtPageDeployment result = service.requestDeploy(9L, QtPageDeployment.TRIGGER_MANUAL);

        assertEquals(pending, result);
        verify(siteMapper, never()).updateSite(any());
        verify(deploymentMapper, never()).insertDeployment(any());
    }

    @Test
    void enqueueLockedSupersedesOlderIntentsAndSnapshotsCurrentConfig()
    {
        QtPageSite site = site(9L, 7L, 1L, 1L);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        doAnswer(invocation -> {
            QtPageDeployment deployment = invocation.getArgument(0);
            deployment.setDeploymentId(20L);
            return 1;
        }).when(deploymentMapper).insertDeployment(any());

        QtPageDeployment created = service.enqueueLocked(site, QtPageDeployment.TRIGGER_CONFIG);

        assertEquals(2L, created.getRequestVersion());
        assertEquals(Long.valueOf(1L), created.getConfigVersion());
        verify(deploymentMapper).markSupersededBefore(9L, 2L);
        ArgumentCaptor<QtPageSite> updated = ArgumentCaptor.forClass(QtPageSite.class);
        verify(siteMapper).updateSite(updated.capture());
        assertEquals(2L, updated.getValue().getRequestVersion());
    }

    @Test
    void lateOldDeploymentCannotReplaceNewTarget()
    throws Exception
    {
        QtPageSite site = site(9L, 7L, 2L, 3L);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment deployment = deployment(20L, 9L, 2L, 1L);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        when(deploymentMapper.selectForUpdate(20L)).thenReturn(deployment);

        service.runDeployment(20L);

        assertEquals(QtPageDeployment.STATUS_SUPERSEDED, deployment.getStatus());
        verify(runtime, never()).gitHubClient();
        assertNull(new QtPageReleaseStore(tempDir).readCurrentRelease(site));
    }

    @Test
    void transientErrorRetriesAt30And120SecondsThenStops()
    {
        QtPageSite site = site(9L, 7L, 1L, 1L);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment deployment = deployment(20L, 9L, 1L, 1L);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        when(deploymentMapper.selectForUpdate(20L)).thenReturn(deployment);
        doThrow(new QtGitHubClient.GitHubClientException(QtGitHubClient.ERROR_NETWORK, true, "network down"))
                .when(gitHubClient).requirePublicRepository(any());

        service.runDeployment(20L);
        assertEquals(1, deployment.getAttemptCount());
        assertEquals(QtPageDeployment.STATUS_PENDING, deployment.getStatus());
        assertEquals(startTime.plusSeconds(30).toEpochMilli(), deployment.getNextAttemptTime().getTime());

        service.setClock(Clock.fixed(startTime.plusSeconds(45), ZoneOffset.UTC));
        service.runDeployment(20L);
        assertEquals(2, deployment.getAttemptCount());
        assertEquals(QtPageDeployment.STATUS_PENDING, deployment.getStatus());
        assertEquals(startTime.plusSeconds(165).toEpochMilli(), deployment.getNextAttemptTime().getTime());

        service.setClock(Clock.fixed(startTime.plusSeconds(300), ZoneOffset.UTC));
        service.runDeployment(20L);
        assertEquals(3, deployment.getAttemptCount());
        assertEquals(QtPageDeployment.STATUS_FAILED, deployment.getStatus());
        assertEquals(QtGitHubClient.ERROR_NETWORK, deployment.getErrorCode());
        assertNull(deployment.getNextAttemptTime());
    }

    @Test
    void executorRejectionLeavesTaskPending()
    {
        QtPageSite site = site(9L, 7L, 1L, 1L);
        QtPageDeployment deployment = deployment(20L, 9L, 1L, 1L);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        when(deploymentMapper.selectDispatchable(any(), anyInt())).thenReturn(java.util.List.of(deployment));
        when(deploymentMapper.selectForUpdate(20L)).thenReturn(deployment);
        java.util.concurrent.ExecutorService rejectingExecutor =
                org.mockito.Mockito.mock(java.util.concurrent.ExecutorService.class);
        doThrow(new RejectedExecutionException("queue full")).when(rejectingExecutor).submit(any(Runnable.class));
        service.setDeployExecutor(rejectingExecutor);

        service.dispatchPending();

        assertEquals(QtPageDeployment.STATUS_PENDING, deployment.getStatus());
        assertEquals(startTime.toEpochMilli(), deployment.getNextAttemptTime().getTime());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void fullPipelinePublishesReleaseAndSwitchesCurrentAtomically()
    throws Exception
    {
        QtPageSite site = site(9L, 7L, 1L, 1L);
        when(siteMapper.selectById(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment deployment = deployment(20L, 9L, 1L, 1L);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        when(deploymentMapper.selectForUpdate(20L)).thenReturn(deployment);
        doAnswer(invocation -> {
            QtPageDeployment d = invocation.getArgument(0);
            d.setDeploymentId(20L);
            return 1;
        }).when(deploymentMapper).insertDeployment(any());

        Path fixture = tempDir.resolve("fixture.tar.gz");
        writeTarFixture(fixture);
        doAnswer(invocation -> {
            Path destination = invocation.getArgument(2);
            Files.copy(fixture, destination, StandardCopyOption.REPLACE_EXISTING);
            return null;
        }).when(gitHubClient).downloadArchive(any(), eq(SHA_A), any());
        when(gitHubClient.getBranchHead(any(), isNull()))
                .thenReturn(new QtGitHubClient.Head(SHA_A, "W/\"e1\"", false));

        service.runDeployment(20L);

        assertEquals(QtPageDeployment.STATUS_SUCCESS, deployment.getStatus());
        assertTrue(deployment.getReleaseAvailable());
        assertEquals(Integer.valueOf(1), deployment.getFileCount());
        assertEquals(Long.valueOf(20L), site.getCurrentReleaseId());
        assertEquals(SHA_A, site.getCurrentSha());
        assertEquals(Long.valueOf(20L), new QtPageReleaseStore(tempDir).readCurrentRelease(site));
        assertTrue(Files.exists(tempDir.resolve("octo-lab/releases/20/index.html")));
        assertFalse(Files.exists(tempDir.resolve("octo-lab/.staging/20")));
    }

    private static void writeTarFixture(Path target) throws Exception
    {
        try (var out = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(
                new java.util.zip.GZIPOutputStream(Files.newOutputStream(target)), "UTF-8"))
        {
            var dir = new org.apache.commons.compress.archivers.tar.TarArchiveEntry("root/");
            out.putArchiveEntry(dir);
            out.closeArchiveEntry();
            byte[] html = "<h1>ok</h1>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            var file = new org.apache.commons.compress.archivers.tar.TarArchiveEntry("root/index.html");
            file.setSize(html.length);
            out.putArchiveEntry(file);
            out.write(html);
            out.closeArchiveEntry();
        }
    }

    private static QtPageSite site(Long siteId, Long userId, Long configVersion, Long requestVersion)
    {
        QtPageSite site = new QtPageSite();
        site.setSiteId(siteId);
        site.setUserId(userId);
        site.setSlug("octo-lab");
        site.setRepoUrl("https://github.com/octo/hello");
        site.setRepoOwner("octo");
        site.setRepoName("hello");
        site.setBranch("pages");
        site.setPublishDir("/");
        site.setConfigVersion(configVersion);
        site.setRequestVersion(requestVersion);
        site.setStatus(QtPageSite.STATUS_ENABLED);
        return site;
    }

    private static QtPageDeployment deployment(Long deploymentId, Long siteId, Long requestVersion, Long configVersion)
    {
        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setDeploymentId(deploymentId);
        deployment.setSiteId(siteId);
        deployment.setRequestVersion(requestVersion);
        deployment.setConfigVersion(configVersion);
        deployment.setRepoOwner("octo");
        deployment.setRepoName("hello");
        deployment.setBranch("pages");
        deployment.setPublishDir("/");
        deployment.setTriggerType(QtPageDeployment.TRIGGER_MANUAL);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        deployment.setAttemptCount(0);
        deployment.setReleaseAvailable(false);
        return deployment;
    }
}
