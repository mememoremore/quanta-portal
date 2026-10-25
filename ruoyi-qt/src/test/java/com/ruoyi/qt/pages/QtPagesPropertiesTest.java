package com.ruoyi.qt.pages;

import org.junit.jupiter.api.Test;
import com.ruoyi.qt.config.QtPagesProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QtPagesPropertiesTest
{
    @Test
    void defaultsKeepPagesDisabledAndBounded()
    {
        QtPagesProperties properties = new QtPagesProperties();
        properties.validate();
        assertFalse(properties.isEnabled());
        assertEquals("page.quantacenter.com", properties.getBaseDomain());
        assertEquals("/srv/quanta-pages", properties.getRootDir());
        assertEquals(300, properties.getPollIntervalSeconds());
        assertEquals(5, properties.getScanIntervalSeconds());
        assertEquals(2, properties.getPollExecutorThreads());
        assertEquals(2, properties.getDeployExecutorThreads());
        assertEquals(8, properties.getExecutorQueueSize());
        assertEquals(100L * 1024 * 1024, properties.getDownloadLimitBytes());
        assertEquals(200L * 1024 * 1024, properties.getExtractedLimitBytes());
        assertEquals(10_000, properties.getMaxFiles());
        assertEquals(20_000, properties.getMaxEntries());
        assertEquals(20L * 1024 * 1024, properties.getMaxFileBytes());
        assertEquals(10, properties.getConnectTimeoutSeconds());
        assertEquals(180, properties.getDownloadTimeoutSeconds());
        assertEquals(300, properties.getDeployTimeoutSeconds());
        assertEquals(3, properties.getMaxAttempts());
        assertEquals(2, properties.getRetryBackoffSeconds().size());
        assertEquals(30, properties.getRetryBackoffSeconds().get(0));
        assertEquals(120, properties.getRetryBackoffSeconds().get(1));
        assertEquals(5, properties.getRetainReleases());
        assertEquals(2L * 1024 * 1024 * 1024, properties.getDiskQuotaBytes());
        assertEquals(32 * 1024, properties.getLogLimitBytes());
    }

    @Test
    void disabledSkipsValidationEvenWithBrokenValues()
    {
        QtPagesProperties properties = new QtPagesProperties();
        properties.setEnabled(false);
        properties.setRootDir("relative/path");
        properties.setPollIntervalSeconds(1);
        properties.validate();
    }

    @Test
    void enabledRequiresAbsolutePosixRoot()
    {
        QtPagesProperties properties = enabled();
        properties.setRootDir("relative/path");
        assertThrows(IllegalStateException.class, properties::validate);

        properties.setRootDir("D:\\quanta-pages");
        assertThrows(IllegalStateException.class, properties::validate);

        properties.setRootDir("/srv/quanta-pages");
        properties.validate();
    }

    @Test
    void enabledRejectsOutOfRangeParameters()
    {
        QtPagesProperties properties = enabled();
        properties.setPollIntervalSeconds(30);
        assertThrows(IllegalStateException.class, properties::validate);

        properties = enabled();
        properties.setScanIntervalSeconds(0);
        assertThrows(IllegalStateException.class, properties::validate);

        properties = enabled();
        properties.setExtractedLimitBytes(1024);
        properties.setDownloadLimitBytes(4096);
        assertThrows(IllegalStateException.class, properties::validate);

        properties = enabled();
        properties.setMaxAttempts(6);
        assertThrows(IllegalStateException.class, properties::validate);

        properties = enabled();
        properties.setRetryBackoffSeconds(java.util.List.of());
        assertThrows(IllegalStateException.class, properties::validate);

        properties = enabled();
        properties.setRetryBackoffSeconds(java.util.List.of(30, 0));
        assertThrows(IllegalStateException.class, properties::validate);

        properties = enabled();
        properties.setRetainReleases(0);
        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void enabledRejectsWildcardBaseDomain()
    {
        QtPagesProperties properties = enabled();
        properties.setBaseDomain("*.quantacenter.com");
        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void toStringNeverContainsGithubToken()
    {
        QtPagesProperties properties = enabled();
        properties.setGithubToken("ghp_super_secret");
        String text = properties.toString();
        assertFalse(text.contains("ghp_super_secret"));
        assertTrue(text.contains("qt-pages") || text.contains("QtPagesProperties"));
    }

    private static QtPagesProperties enabled()
    {
        QtPagesProperties properties = new QtPagesProperties();
        properties.setEnabled(true);
        return properties;
    }
}
