package com.ruoyi.qt.pages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.qt.domain.QtPageSite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QtPageReleaseStoreTest
{
    @TempDir
    Path tempDir;

    private QtPageReleaseStore store;

    @BeforeEach
    void setUp()
    {
        store = new QtPageReleaseStore(tempDir);
    }

    private QtPageSite site(String slug)
    {
        QtPageSite site = new QtPageSite();
        site.setSiteId(9L);
        site.setSlug(slug);
        return site;
    }

    @Test
    void siteLockIsMutuallyExclusiveAndReentrantAfterClose()
    throws Exception
    {
        QtPageSite site = site("lock-lab");
        Optional<QtPageReleaseStore.SiteLock> first = store.tryLock(site);
        assertTrue(first.isPresent());
        assertFalse(store.tryLock(site).isPresent());
        first.get().close();
        Optional<QtPageReleaseStore.SiteLock> second = store.tryLock(site);
        assertTrue(second.isPresent());
        second.get().close();
    }

    @Test
    void readCurrentReleaseIsNullBeforeFirstPublish()
    throws Exception
    {
        assertNull(store.readCurrentRelease(site("fresh-lab")));
    }

    @Test
    void prepareMovesValidatedStagingIntoImmutableRelease()
    throws Exception
    {
        QtPageSite site = site("move-lab");
        Path staging = store.stagingDirectory(site, 20L);
        Files.createDirectories(staging);
        Files.writeString(staging.resolve("index.html"), "<h1>hi</h1>");

        QtStaticArchiveValidator.ValidatedArtifact artifact =
                new QtStaticArchiveValidator.ValidatedArtifact(staging, 1, 10);
        Path release = store.prepare(site, 20L, artifact);

        assertEquals(store.releaseDirectory(site, 20L), release);
        assertTrue(Files.exists(release.resolve("index.html")));
        assertFalse(Files.exists(staging), "staging must be consumed after prepare");
        assertFalse(Files.exists(store.stagingRoot(site).resolve("20")));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void switchCurrentIsRejectedForMissingRelease()
    throws Exception
    {
        QtPageSite site = site("switch-lab");
        Path release = store.releaseDirectory(site, 1L);
        Files.createDirectories(release);
        Files.writeString(release.resolve("index.html"), "v1");
        store.switchCurrent(site, 1L);
        assertEquals(Long.valueOf(1L), store.readCurrentRelease(site));

        assertThrows(Exception.class, () -> store.switchCurrent(site, 99L));
        assertEquals(Long.valueOf(1L), store.readCurrentRelease(site), "failed switch must keep old current");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void currentIsARelativeSymlinkOnlyTrustedWhenPlatformGenerated()
    throws Exception
    {
        QtPageSite site = site("linux-lab");
        for (long id : new long[] { 1L, 2L })
        {
            Path release = store.releaseDirectory(site, id);
            Files.createDirectories(release);
            Files.writeString(release.resolve("index.html"), "v" + id);
        }
        store.switchCurrent(site, 1L);
        Path current = store.currentLink(site);
        assertTrue(Files.isSymbolicLink(current));
        assertEquals("releases/1", Files.readSymbolicLink(current).toString());
        assertEquals("v1", Files.readString(current.resolve("index.html")));

        store.switchCurrent(site, 2L);
        assertEquals("releases/2", Files.readSymbolicLink(store.currentLink(site)).toString());
        assertEquals("v2", Files.readString(store.currentLink(site).resolve("index.html")));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void brokenOrForeignCurrentIsNotTrusted()
    throws Exception
    {
        QtPageSite site = site("broken-lab");
        Files.createDirectories(store.siteDirectory(site));
        java.nio.file.Files.createSymbolicLink(store.currentLink(site), Path.of("../../outside"));
        assertNull(store.readCurrentRelease(site), "platform must not trust foreign link targets");
    }

    @Test
    void pruneKeepsProtectedReleasesAndRemovesOthers()
    throws Exception
    {
        QtPageSite site = site("prune-lab");
        for (long id : new long[] { 1L, 2L, 3L })
        {
            Path release = store.releaseDirectory(site, id);
            Files.createDirectories(release);
            Files.writeString(release.resolve("index.html"), "v" + id);
        }
        store.prune(site, Set.of(2L));
        assertTrue(Files.exists(store.releaseDirectory(site, 2L)));
        assertFalse(Files.exists(store.releaseDirectory(site, 1L)));
        assertFalse(Files.exists(store.releaseDirectory(site, 3L)));
    }

    @Test
    void pruneCleansLeftoverStaging()
    throws Exception
    {
        QtPageSite site = site("prune-stage-lab");
        Path staging = store.stagingDirectory(site, 77L);
        Files.createDirectories(staging);
        Files.writeString(staging.resolve("partial.html"), "half");
        store.prune(site, Set.of());
        assertFalse(Files.exists(store.stagingRoot(site).resolve("77")));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void readCurrentReleaseParsesPlatformGeneratedLink()
    throws Exception
    {
        QtPageSite site = site("parse-lab");
        Path release = store.releaseDirectory(site, 5L);
        Files.createDirectories(release);
        Files.writeString(release.resolve("index.html"), "v5");
        store.switchCurrent(site, 5L);
        assertNotNull(store.readCurrentRelease(site));
        assertEquals(Long.valueOf(5L), store.readCurrentRelease(site));
        assertTrue(store.currentLink(site).toAbsolutePath().startsWith(store.siteDirectory(site).toAbsolutePath()));
    }
}
