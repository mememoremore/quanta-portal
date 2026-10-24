package com.ruoyi.qt.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.mapper.QtPageDeploymentMapper;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtPagesAccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtPageDeployServiceTest
{
    @Mock
    private QtPageSiteMapper siteMapper;

    @Mock
    private QtPageDeploymentMapper deploymentMapper;

    @Mock
    private QtPagesAccess access;

    @InjectMocks
    private QtPageDeployServiceImpl service;

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
    void queuedManualWithOutdatedSnapshotDoesNotBlockNewIntent()
    {
        QtPageSite site = site(9L, 7L, 1L, 1L);
        when(access.requireOwnedSite(9L)).thenReturn(site);
        when(siteMapper.selectForUpdate(9L)).thenReturn(site);
        QtPageDeployment stale = new QtPageDeployment();
        stale.setBranch("old-branch");
        stale.setRepoOwner("octo");
        stale.setRepoName("hello");
        stale.setPublishDir("/");
        when(deploymentMapper.selectReusableManual(9L)).thenReturn(stale);
        doAnswer(invocation -> {
            QtPageDeployment deployment = invocation.getArgument(0);
            deployment.setDeploymentId(10L);
            return 1;
        }).when(deploymentMapper).insertDeployment(any());

        QtPageDeployment created = service.requestDeploy(9L, QtPageDeployment.TRIGGER_MANUAL);

        assertEquals(2L, created.getRequestVersion());
        verify(siteMapper).updateSite(site);
        verify(deploymentMapper).markSupersededBefore(9L, 2L);
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
        assertEquals("octo", created.getRepoOwner());
        assertEquals("hello", created.getRepoName());
        assertEquals("pages", created.getBranch());
        assertEquals("/", created.getPublishDir());
        verify(deploymentMapper).markSupersededBefore(9L, 2L);
        ArgumentCaptor<QtPageSite> updated = ArgumentCaptor.forClass(QtPageSite.class);
        verify(siteMapper).updateSite(updated.capture());
        assertEquals(2L, updated.getValue().getRequestVersion());
    }

    @Test
    void enqueueLockedRejectsNullSite()
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.enqueueLocked(null, QtPageDeployment.TRIGGER_CREATE));
        assertEquals(HttpStatus.NOT_FOUND, exception.getCode());
        assertTrue(exception.getMessage().contains("站点不存在"));
    }

    private static QtPageSite site(Long siteId, Long userId, Long configVersion, Long requestVersion)
    {
        QtPageSite site = new QtPageSite();
        site.setSiteId(siteId);
        site.setUserId(userId);
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
}
