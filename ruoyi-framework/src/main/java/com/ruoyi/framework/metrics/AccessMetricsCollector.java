package com.ruoyi.framework.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

/**
 * 访问指标内存聚合器。请求路径只做计数，由定时任务 snapshot 后落库。
 */
@Component
public class AccessMetricsCollector
{
    private static final String GLOBAL_KEY = AccessMetricsRow.GLOBAL_METHOD + "\t" + AccessMetricsRow.GLOBAL_URI;
    private static final int URI_MAX_LEN = 255;

    private volatile ConcurrentHashMap<String, UriBucket> buckets = new ConcurrentHashMap<String, UriBucket>();
    private volatile long windowStartMs = System.currentTimeMillis();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger inFlightMax = new AtomicInteger();

    public void beginRequest()
    {
        int now = inFlight.incrementAndGet();
        inFlightMax.accumulateAndGet(now, Math::max);
    }

    public void endRequest()
    {
        inFlight.updateAndGet(v -> Math.max(0, v - 1));
    }

    public void record(String uri, String method, int status, long costMs)
    {
        if (uri == null || uri.isEmpty())
        {
            return;
        }
        String safeUri = uri.length() > URI_MAX_LEN ? uri.substring(0, URI_MAX_LEN) : uri;
        String safeMethod = (method == null || method.isEmpty()) ? "?" : method;
        long cost = Math.max(0L, costMs);
        bucket(GLOBAL_KEY).add(status, cost);
        bucket(safeMethod + "\t" + safeUri).add(status, cost);
    }

    public AccessMetricsSnapshot peek(int topUris)
    {
        return buildSnapshot(buckets, windowStartMs, System.currentTimeMillis(), inFlightMax.get(), topUris);
    }

    public AccessMetricsSnapshot snapshotAndReset(int topUris)
    {
        long now = System.currentTimeMillis();
        ConcurrentHashMap<String, UriBucket> old = buckets;
        buckets = new ConcurrentHashMap<String, UriBucket>();
        long start = windowStartMs;
        windowStartMs = now;
        int maxFlight = inFlightMax.getAndSet(Math.max(0, inFlight.get()));
        return buildSnapshot(old, start, now, maxFlight, topUris);
    }

    private UriBucket bucket(String key)
    {
        return buckets.computeIfAbsent(key, k -> new UriBucket());
    }

    private AccessMetricsSnapshot buildSnapshot(ConcurrentHashMap<String, UriBucket> source, long startMs,
            long endMs, int maxFlight, int topUris)
    {
        long windowMs = Math.max(1L, endMs - startMs);
        AccessMetricsSnapshot snapshot = new AccessMetricsSnapshot();
        snapshot.setWindowStartMs(startMs);
        snapshot.setWindowEndMs(endMs);
        snapshot.setWindowMs(windowMs);
        snapshot.setSnapshotTime(new Date(endMs));

        UriBucket globalBucket = source.get(GLOBAL_KEY);
        AccessMetricsRow global = toRow(AccessMetricsRow.GLOBAL_URI, AccessMetricsRow.GLOBAL_METHOD, globalBucket,
                windowMs);
        global.setInFlightMax(maxFlight);
        global.setWindowMs(windowMs);
        snapshot.setGlobal(global);

        List<AccessMetricsRow> rows = new ArrayList<AccessMetricsRow>();
        for (Map.Entry<String, UriBucket> entry : source.entrySet())
        {
            if (GLOBAL_KEY.equals(entry.getKey()))
            {
                continue;
            }
            int tab = entry.getKey().indexOf('\t');
            if (tab < 0)
            {
                continue;
            }
            String method = entry.getKey().substring(0, tab);
            String uri = entry.getKey().substring(tab + 1);
            rows.add(toRow(uri, method, entry.getValue(), windowMs));
        }
        rows.sort(Comparator.comparingLong(AccessMetricsRow::getRequestCount).reversed());
        int limit = Math.max(0, topUris);
        snapshot.setTopUris(rows.size() > limit ? new ArrayList<AccessMetricsRow>(rows.subList(0, limit)) : rows);
        return snapshot;
    }

    private AccessMetricsRow toRow(String uri, String method, UriBucket bucket, long windowMs)
    {
        AccessMetricsRow row = new AccessMetricsRow();
        row.setUri(uri);
        row.setMethod(method);
        row.setWindowMs(windowMs);
        if (bucket == null)
        {
            row.setQps(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return row;
        }
        long count = bucket.count.sum();
        long totalCost = bucket.totalCost.sum();
        row.setRequestCount(count);
        row.setError4xx(bucket.error4xx.sum());
        row.setError5xx(bucket.error5xx.sum());
        row.setAvgCostMs(count == 0 ? 0 : (int) (totalCost / count));
        row.setMaxCostMs((int) Math.min(Integer.MAX_VALUE, bucket.maxCost.get()));
        row.setQps(qps(count, windowMs));
        return row;
    }

    static BigDecimal qps(long count, long windowMs)
    {
        if (windowMs <= 0)
        {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(1000)).divide(BigDecimal.valueOf(windowMs), 2,
                RoundingMode.HALF_UP);
    }

    static final class UriBucket
    {
        private final LongAdder count = new LongAdder();
        private final LongAdder error4xx = new LongAdder();
        private final LongAdder error5xx = new LongAdder();
        private final LongAdder totalCost = new LongAdder();
        private final AtomicLong maxCost = new AtomicLong();

        void add(int status, long costMs)
        {
            count.increment();
            totalCost.add(costMs);
            maxCost.accumulateAndGet(costMs, Math::max);
            if (status >= 500)
            {
                error5xx.increment();
            }
            else if (status >= 400)
            {
                error4xx.increment();
            }
        }
    }
}
