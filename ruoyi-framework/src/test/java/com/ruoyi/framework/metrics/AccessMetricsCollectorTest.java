package com.ruoyi.framework.metrics;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessMetricsCollectorTest
{
    @Test
    void snapshotComputesTotalsThenResets() throws InterruptedException
    {
        AccessMetricsCollector collector = new AccessMetricsCollector();
        for (int i = 0; i < 10; i++)
        {
            collector.record("/qt/book/list", "GET", 200, 50);
        }
        collector.record("/login", "POST", 401, 20);
        collector.record("/qt/book/{bookId}", "GET", 500, 80);
        Thread.sleep(50);

        AccessMetricsSnapshot first = collector.snapshotAndReset(20);
        AccessMetricsRow global = first.getGlobal();
        assertEquals(12, global.getRequestCount());
        assertEquals(1, global.getError4xx());
        assertEquals(1, global.getError5xx());
        assertEquals(50, global.getAvgCostMs());
        assertEquals(80, global.getMaxCostMs());
        assertEquals(3, first.getTopUris().size());
        assertEquals("/qt/book/list", first.getTopUris().get(0).getUri());
        assertTrue(global.getQps().compareTo(BigDecimal.ZERO) > 0);

        AccessMetricsSnapshot second = collector.snapshotAndReset(20);
        assertEquals(0, second.getGlobal().getRequestCount());
        assertTrue(second.getTopUris().isEmpty());
        assertEquals(new BigDecimal("0.00"), second.getGlobal().getQps());
    }

    @Test
    void peekDoesNotResetCounters()
    {
        AccessMetricsCollector collector = new AccessMetricsCollector();
        collector.record("/dashboard/stats", "GET", 200, 10);
        assertEquals(1, collector.peek(5).getGlobal().getRequestCount());
        assertEquals(1, collector.peek(5).getGlobal().getRequestCount());
        assertEquals(1, collector.snapshotAndReset(5).getGlobal().getRequestCount());
    }

    @Test
    void topUrisRespectsLimit()
    {
        AccessMetricsCollector collector = new AccessMetricsCollector();
        collector.record("/a", "GET", 200, 1);
        collector.record("/b", "GET", 200, 1);
        collector.record("/c", "GET", 200, 1);
        assertEquals(2, collector.peek(2).getTopUris().size());
    }
}
