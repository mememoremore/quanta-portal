package com.ruoyi.qt.pages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.qt.domain.QtPageSite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QtPageReleaseRetentionTest
{
    @TempDir
    Path tempDir;

    private QtPageReleaseStore store;

    private QtPageSite site;

    @BeforeEach
    void setUp()
    {
        store = new QtPageReleaseStore(tempDir);
        site = new QtPageSite();
        site.setSiteId(9L);
        site.setSlug("retain-lab");
    }

    @Test
    void usageCoversStagingAndAllReleases()
    throws Exception
    {
        Files.createDirectories(store.releaseDirectory(site, 1L));
        Files.writeString(store.releaseDirectory(site, 1L).resolve("index.html"), "12345");
        Files.createDirectories(store.stagingDirectory(site, 7L));
        Files.writeString(store.stagingDirectory(site, 7L).resolve("archive.tar.gz"), "1234567");

        assertEquals(12L, store.siteUsageBytes(site));
    }

    @Test
    void listDeletableReleasesOnlyReturnsUnprotectedNumericDirectories()
    throws Exception
    {
        for (long id : new long[] { 1L, 2L, 3L, 4L, 5L, 6L })
        {
            Files.createDirectories(store.releaseDirectory(site, id));
        }
        Files.createDirectories(store.siteDirectory(site).resolve("releases/not-a-release"));

        List<Long> deletable = store.listDeletableReleases(site, Set.of(2L, 5L));

        assertEquals(List.of(1L, 3L, 4L, 6L), deletable);
    }

    @Test
    void pruneKeepsProtectedAndRemovesVictims()
    throws Exception
    {
        for (long id : new long[] { 1L, 2L, 3L })
        {
            Files.createDirectories(store.releaseDirectory(site, id));
            Files.writeString(store.releaseDirectory(site, id).resolve("index.html"), "v" + id);
        }

        store.prune(site, Set.of(2L));

        assertFalse(Files.exists(store.releaseDirectory(site, 1L)));
        assertTrue(Files.exists(store.releaseDirectory(site, 2L)));
        assertFalse(Files.exists(store.releaseDirectory(site, 3L)));
    }

    @Test
    void pruneNeverTouchesNonNumericEntries()
    throws Exception
    {
        Files.createDirectories(store.siteDirectory(site).resolve("releases/keep-me"));
        store.prune(site, Set.of());
        assertTrue(Files.exists(store.siteDirectory(site).resolve("releases/keep-me")));
    }
}
