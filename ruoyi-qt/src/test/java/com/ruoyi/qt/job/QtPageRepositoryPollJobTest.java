package com.ruoyi.qt.job;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.ruoyi.qt.pages.QtGitHubClient;
import com.ruoyi.qt.pages.QtPageReleaseStore;
import com.ruoyi.qt.pages.QtPagesAccess;
import com.ruoyi.qt.pages.QtPagesRuntime;
import com.ruoyi.qt.service.IQtPageDeployService;
import com.ruoyi.qt.service.impl.QtPageDeployServiceImpl;
import com.ruoyi.qt.service.impl.QtPageDeployTransactions;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
class QtPageRepositoryPollJobTest
{
    private static final String SHA_A = "a".repeat(40);
    private static final String SHA_B = "b".repeat(40);
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @TempDir
    Path tempDir;

    @Mock
    private QtPageSiteMapper siteMapper;

    @Mock
    private QtPageDeploymentMapper deploymentMapper;

    @Mock
    private QtPagesRuntime runtime;

    @Mock
    private QtGitHubClient gitHubClient;

    private QtPageSite site;

    @BeforeEach
    void setUp()
    {
        QtPagesProperties properties = new QtPagesProperties();
        when(runtime.isReady()).thenReturn(true);
        when(runtime.properties()).thenReturn(properties);
        when(runtime.store()).thenReturn(new QtPageReleaseStore(tempDir));
        when(runtime.gitHubClient()).thenReturn(gitHubClient);

        site = new QtPageSite();
        site.setSiteId(9L);
        site.setSlug("poll-lab");
        site.setRepoOwner("octo");
        site.setRepoName("hello");
        site.setBranch("pages");
        site.setPublishDir("/");
        site.setConfigVersion(1L);
        site.setRequestVersion(1L);
        site.setStatus(QtPageSite.STATUS_ENABLED);
        site.setNextPollTime(new java.util.Date(0));
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
    }

    private QtPageDeployServiceImpl newService()
    {
        QtPageDeployServiceImpl service = new QtPageDeployServiceImpl();
        ReflectionTestUtils.setField(service, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(service, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(service, "access", org.mockito.Mockito.mock(QtPagesAccess.class));
        ReflectionTestUtils.setField(service, "runtime", runtime);
        QtPageDeployTransactions transactions = new QtPageDeployTransactions();
        ReflectionTestUtils.setField(transactions, "siteMapper", siteMapper);
        ReflectionTestUtils.setField(transactions, "deploymentMapper", deploymentMapper);
        ReflectionTestUtils.setField(transactions, "runtime", runtime);
        ReflectionTestUtils.setField(service, "transactions", transactions);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
        return service;
    }

    @Test
    void newHeadSchedulesExactlyOneDeployment()
    {
        when(siteMapper.selectDueSites(any(), eq(50))).thenReturn(java.util.List.of(site));
        when(gitHubClient.getBranchHead(any(), isNull()))
                .thenReturn(new QtGitHubClient.Head(SHA_A, "W/\"etag-1\"", false));
        when(gitHubClient.getBranchHead(any(), eq("W/\"etag-1\"")))
                .thenReturn(new QtGitHubClient.Head(null, "W/\"etag-1\"", true));
        doAnswer(invocation -> {
            QtPageDeployment deployment = invocation.getArgument(0);
            deployment.setDeploymentId(100L);
            return 1;
        }).when(deploymentMapper).insertDeployment(any());

        QtPageDeployServiceImpl service = newService();
        service.pollDueSites();
        service.pollDueSites();

        ArgumentCaptor<QtPageDeployment> inserted = ArgumentCaptor.forClass(QtPageDeployment.class);
        verify(deploymentMapper, times(1)).insertDeployment(inserted.capture());
        assertEquals(QtPageDeployment.TRIGGER_POLL, inserted.getValue().getTriggerType());
        assertEquals(SHA_A, inserted.getValue().getCommitSha());
        assertEquals(Long.valueOf(2L), inserted.getValue().getRequestVersion());
        assertEquals(SHA_A, site.getDesiredSha());
        assertEquals("W/\"etag-1\"", site.getPollEtag());
        assertEquals(SHA_A, site.getLastObservedSha());
        assertNull(site.getLastCheckError());
        assertEquals(NOW.plusSeconds(300).toEpochMilli(), site.getNextPollTime().getTime());
    }

    @Test
    void branchMissingKeepsCurrentAndRecordsError()
    {
        when(siteMapper.selectDueSites(any(), eq(50))).thenReturn(java.util.List.of(site));
        when(gitHubClient.getBranchHead(any(), isNull()))
                .thenThrow(new QtGitHubClient.GitHubClientException(
                        QtGitHubClient.ERROR_BRANCH_NOT_FOUND, false, "branch missing"));

        newService().pollDueSites();

        verify(deploymentMapper, never()).insertDeployment(any());
        assertTrue(site.getLastCheckError().contains(QtGitHubClient.ERROR_BRANCH_NOT_FOUND));
        assertEquals(NOW.plusSeconds(300).toEpochMilli(), site.getNextPollTime().getTime());
        assertNull(site.getDesiredSha());
    }

    @Test
    void rateLimitedDelaysNextPollToRetryAt()
    {
        when(siteMapper.selectDueSites(any(), eq(50))).thenReturn(java.util.List.of(site));
        when(gitHubClient.getBranchHead(any(), isNull()))
                .thenThrow(new QtGitHubClient.GitHubClientException(
                        QtGitHubClient.ERROR_RATE_LIMITED, true, NOW.plusSeconds(600), "rate limited"));

        newService().pollDueSites();

        verify(deploymentMapper, never()).insertDeployment(any());
        assertEquals(NOW.plusSeconds(600).toEpochMilli(), site.getNextPollTime().getTime());
        assertTrue(site.getLastCheckError().contains(QtGitHubClient.ERROR_RATE_LIMITED));
    }

    @Test
    void existingUnresolvedIntentSuppressesDuplicatePollIntent()
    {
        when(siteMapper.selectDueSites(any(), eq(50))).thenReturn(java.util.List.of(site));
        when(gitHubClient.getBranchHead(any(), isNull()))
                .thenReturn(new QtGitHubClient.Head(SHA_A, null, false));
        when(deploymentMapper.selectUnresolvedForSite(9L)).thenReturn(new QtPageDeployment());

        newService().pollDueSites();

        verify(deploymentMapper, never()).insertDeployment(any());
        assertEquals(SHA_A, site.getDesiredSha());
        assertEquals(Long.valueOf(1L), site.getRequestVersion());
    }

    @Test
    void unchangedBranchHeadDoesNotUndoRollback()
    {
        // 回滚后 current 指回旧提交 R，分支头 H 与 desired 一致：轮询不产生新意图
        site.setCurrentSha(SHA_A);
        site.setDesiredSha(SHA_B);
        site.setCurrentReleaseId(20L);
        when(siteMapper.selectDueSites(any(), eq(50))).thenReturn(java.util.List.of(site));
        when(gitHubClient.getBranchHead(any(), isNull()))
                .thenReturn(new QtGitHubClient.Head(SHA_B, null, false));

        newService().pollDueSites();

        verify(deploymentMapper, never()).insertDeployment(any());
        assertEquals(Long.valueOf(1L), site.getRequestVersion());
    }

    @Test
    void suspendedSiteIsNotPolled()
    {
        site.setStatus(QtPageSite.STATUS_SUSPENDED);
        when(siteMapper.selectDueSites(any(), eq(50))).thenReturn(java.util.List.of(site));
        newService().pollDueSites();
        verify(gitHubClient, never()).getBranchHead(any(), anyString());
        verify(gitHubClient, never()).getBranchHead(any(), isNull());
    }

    @Test
    void jobDelegatesToServiceAndSwallowsFailures()
    {
        @SuppressWarnings("unchecked")
        IQtPageDeployService deployService = org.mockito.Mockito.mock(IQtPageDeployService.class);
        QtPageRepositoryPollJob job = new QtPageRepositoryPollJob(deployService);
        job.poll();
        verify(deployService).pollDueSites();

        doThrow(new IllegalStateException("boom")).when(deployService).pollDueSites();
        job.poll();
        verify(deployService, times(2)).pollDueSites();
    }
}
