package com.ruoyi.framework.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessMetricsFilterTest
{
    @Test
    void skipsStaticDocsAndSelf()
    {
        assertTrue(AccessMetricsFilter.isExcludedPath("/profile/upload/a.png"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/swagger-ui.html"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/swagger-ui/index.html"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/v3/api-docs"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/druid/index.html"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/qt/metrics/latest"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/qt/metrics/list"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/favicon.ico"));
        assertTrue(AccessMetricsFilter.isExcludedPath("/error"));
        assertFalse(AccessMetricsFilter.isExcludedPath("/qt/book/list"));
        assertFalse(AccessMetricsFilter.isExcludedPath("/login"));
        assertFalse(AccessMetricsFilter.isExcludedPath("/dashboard/stats"));
    }
}
