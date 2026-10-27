package com.ruoyi.qt.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.domain.QtPageSiteCreateBody;
import com.ruoyi.qt.domain.QtPageSiteSaveResult;
import com.ruoyi.qt.domain.QtPageSiteUpdateBody;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtPagesAccess;
import com.ruoyi.qt.pages.QtPagesRuntime;
import com.ruoyi.qt.service.IQtPageDeployService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtPageSiteServiceTest
{
    @Mock
    private QtPageSiteMapper siteMapper;

    @Mock
    private QtPagesAccess access;

    @Mock
    private IQtPageDeployService deployService;

    @Mock
    private QtPagesRuntime runtime;

    @InjectMocks
    private QtPageSiteServiceImpl service;

    @Test
    void memberCannotReadOrMutateAnotherSite()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        QtPageSite foreign = new QtPageSite();
        foreign.setSiteId(99L);
        foreign.setUserId(8L);
        when(siteMapper.selectForUpdate(99L)).thenReturn(foreign);

        QtPageSiteUpdateBody body = new QtPageSiteUpdateBody();
        body.setRepoUrl("https://github.com/octo/hello");
        ServiceException exception = assertThrows(ServiceException.class, () -> service.update(99L, body));
        assertEquals(HttpStatus.NOT_FOUND, exception.getCode());
        verify(deployService, never()).enqueueLocked(any(), any());
    }

    @Test
    void getOwnSiteReturnsNullBeforeFirstCreate()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        when(siteMapper.selectByUserId(7L)).thenReturn(null);
        assertNull(service.getOwnSite());
    }

    @Test
    void memberCanOnlyHaveOneSite()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        when(siteMapper.selectByUserId(7L)).thenReturn(new QtPageSite());
        ServiceException exception = assertThrows(ServiceException.class, () -> service.create(body("https://github.com/octo/hello", null, null, null)));
        assertEquals(HttpStatus.CONFLICT, exception.getCode());
    }

    @Test
    void rejectInvalidSlugAndRepositoryUrl()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        when(siteMapper.selectByUserId(7L)).thenReturn(null);

        assertBadRequest(body("https://github.com/octo/hello", "ab", null, null));
        assertBadRequest(body("https://github.com/octo/hello", "www", null, null));
        assertBadRequest(body("https://github.com/octo/hello", "xn--abc", null, null));
        assertBadRequest(body("https://github.com/octo/hello", "-abc", null, null));
        assertBadRequest(body("https://github.com/octo/hello", "a b", null, null));
        assertBadRequest(body("https://github.com/octo/hello", null, "bad branch", null));
        assertBadRequest(body("https://github.com/octo/hello", null, "-feature", null));
        assertBadRequest(body("https://github.com/octo/hello", null, "a//b", null));
        assertBadRequest(body("https://github.com/octo/hello", null, null, "../etc"));
        assertBadRequest(body("https://github.com/octo/hello", null, null, ".hidden"));
        assertBadRequest(body("https://github.com/octo/hello", null, null, "templates/classic"));
        assertBadRequest(body("https://github.com/octo/hello", null, null, "/templates/../classic"));
        assertBadRequest(body("https://github.com/octo/hello", null, null, "/templates//classic"));
        assertBadRequest(body("https://github.com/octo/hello", null, null, "/.git"));
        assertBadRequest(body("https://github.com/octo/hello", null, null, "/a/.hidden"));
        assertBadRequest(body("https://github.com/octo/hello", null, null, "/templates\\classic"));

        assertBadRequest(body("http://github.com/octo/hello", null, null, null));
        assertBadRequest(body("https://gitlab.com/octo/hello", null, null, null));
        assertBadRequest(body("https://user:pass@github.com/octo/hello", null, null, null));
        assertBadRequest(body("https://github.com:8443/octo/hello", null, null, null));
        assertBadRequest(body("https://github.com/octo/hello?x=1", null, null, null));
        assertBadRequest(body("https://github.com/octo", null, null, null));
        assertBadRequest(body("https://github.com/octo/hello/extra", null, null, null));
        assertBadRequest(body("", null, null, null));
    }

    @Test
    void acceptsNestedTemplatePublishDir()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        when(siteMapper.selectByUserId(7L)).thenReturn(null);
        when(siteMapper.insertSite(any())).thenReturn(1);
        when(deployService.enqueueLocked(any(), eq(QtPageDeployment.TRIGGER_CREATE)))
                .thenAnswer(invocation -> new QtPageDeployment());

        service.create(body("https://github.com/octo/hello", "my-space", "main", "/templates/classic/"));

        ArgumentCaptor<QtPageSite> site = ArgumentCaptor.forClass(QtPageSite.class);
        verify(siteMapper).insertSite(site.capture());
        assertEquals("/templates/classic", site.getValue().getPublishDir());
        assertEquals("main", site.getValue().getBranch());
    }

    @Test
    void createAppliesDefaultsAndSnapshots()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        when(siteMapper.selectByUserId(7L)).thenReturn(null);
        when(siteMapper.insertSite(any())).thenReturn(1);
        when(deployService.enqueueLocked(any(), eq(QtPageDeployment.TRIGGER_CREATE)))
                .thenAnswer(invocation -> new QtPageDeployment());

        QtPageSiteSaveResult result = service.create(body("https://github.com/Octo/Hello.git", null, null, null));

        ArgumentCaptor<QtPageSite> site = ArgumentCaptor.forClass(QtPageSite.class);
        verify(siteMapper).insertSite(site.capture());
        assertEquals("https://github.com/Octo/Hello", site.getValue().getRepoUrl());
        assertEquals("Octo", site.getValue().getRepoOwner());
        assertEquals("Hello", site.getValue().getRepoName());
        assertEquals("pages", site.getValue().getBranch());
        assertEquals("/", site.getValue().getPublishDir());
        assertEquals(Long.valueOf(1L), site.getValue().getConfigVersion());
        assertEquals(Long.valueOf(0L), site.getValue().getRequestVersion());
        assertEquals(QtPageSite.STATUS_ENABLED, site.getValue().getStatus());
        assertNotNull(site.getValue().getNextPollTime());
        assertTrue(site.getValue().getSlug().matches("^[a-z0-9]{8}$"));
        verify(deployService).enqueueLocked(site.getValue(), QtPageDeployment.TRIGGER_CREATE);
        assertNotNull(result.deployment());
    }

    @Test
    void explicitSlugIsNormalizedAndConflictReturns409()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        when(siteMapper.selectByUserId(7L)).thenReturn(null);
        when(siteMapper.insertSite(any())).thenThrow(new DuplicateKeyException("uk_qt_page_slug"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.create(body("https://github.com/octo/hello", "Alice-Lab", null, null)));
        assertEquals(HttpStatus.CONFLICT, exception.getCode());

        ArgumentCaptor<QtPageSite> site = ArgumentCaptor.forClass(QtPageSite.class);
        verify(siteMapper).insertSite(site.capture());
        assertEquals("alice-lab", site.getValue().getSlug());
    }

    @Test
    void generatedSlugRetriesOnConflict()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        when(siteMapper.selectByUserId(7L)).thenReturn(null);
        java.util.List<String> attemptedSlugs = new java.util.ArrayList<>();
        when(siteMapper.insertSite(any())).thenAnswer(invocation -> {
            attemptedSlugs.add(((QtPageSite) invocation.getArgument(0)).getSlug());
            throw new DuplicateKeyException("uk_qt_page_slug");
        }).thenAnswer(invocation -> {
            attemptedSlugs.add(((QtPageSite) invocation.getArgument(0)).getSlug());
            throw new DuplicateKeyException("uk_qt_page_slug");
        }).thenAnswer(invocation -> {
            attemptedSlugs.add(((QtPageSite) invocation.getArgument(0)).getSlug());
            return 1;
        });
        when(deployService.enqueueLocked(any(), eq(QtPageDeployment.TRIGGER_CREATE)))
                .thenAnswer(invocation -> new QtPageDeployment());

        service.create(body("https://github.com/octo/hello", null, null, null));

        assertEquals(3, attemptedSlugs.size());
        assertNotEquals(attemptedSlugs.get(0), attemptedSlugs.get(2));
    }

    @Test
    void configChangeSnapshotsSourceAndAdvancesVersions()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        QtPageSite existing = new QtPageSite();
        existing.setSiteId(9L);
        existing.setUserId(7L);
        existing.setRepoUrl("https://github.com/octo/hello");
        existing.setRepoOwner("octo");
        existing.setRepoName("hello");
        existing.setBranch("pages");
        existing.setPublishDir("/");
        existing.setConfigVersion(3L);
        existing.setRequestVersion(5L);
        when(siteMapper.selectForUpdate(9L)).thenReturn(existing);
        when(deployService.enqueueLocked(any(), eq(QtPageDeployment.TRIGGER_CONFIG)))
                .thenAnswer(invocation -> new QtPageDeployment());

        QtPageSiteUpdateBody body = new QtPageSiteUpdateBody();
        body.setRepoUrl("https://github.com/octo/site.git");
        body.setPublishDir("/dist");
        service.update(9L, body);

        ArgumentCaptor<QtPageSite> site = ArgumentCaptor.forClass(QtPageSite.class);
        verify(siteMapper).updateSite(site.capture());
        assertEquals("https://github.com/octo/site", site.getValue().getRepoUrl());
        assertEquals("octo", site.getValue().getRepoOwner());
        assertEquals("site", site.getValue().getRepoName());
        assertEquals("/dist", site.getValue().getPublishDir());
        assertEquals(Long.valueOf(4L), site.getValue().getConfigVersion());
        // request_version 只由 enqueueLocked 推进
        assertEquals(Long.valueOf(5L), site.getValue().getRequestVersion());
        verify(deployService).enqueueLocked(site.getValue(), QtPageDeployment.TRIGGER_CONFIG);
    }

    @Test
    void updateRejectsSlugFieldAbsenceAndEmptyBody()
    {
        when(access.requireCurrentMemberId()).thenReturn(7L);
        QtPageSite existing = new QtPageSite();
        existing.setSiteId(9L);
        existing.setUserId(7L);
        existing.setConfigVersion(1L);
        when(siteMapper.selectForUpdate(9L)).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.update(9L, new QtPageSiteUpdateBody()));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getCode());
    }

    private void assertBadRequest(QtPageSiteCreateBody body)
    {
        ServiceException exception = assertThrows(ServiceException.class, () -> service.create(body));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getCode());
    }

    private static QtPageSiteCreateBody body(String repoUrl, String slug, String branch, String publishDir)
    {
        QtPageSiteCreateBody create = new QtPageSiteCreateBody();
        create.setRepoUrl(repoUrl);
        create.setSlug(slug);
        create.setBranch(branch);
        create.setPublishDir(publishDir);
        return create;
    }
}
