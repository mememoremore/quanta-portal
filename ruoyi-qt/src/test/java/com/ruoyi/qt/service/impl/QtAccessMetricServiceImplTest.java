package com.ruoyi.qt.service.impl;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.framework.metrics.AccessMetricsCollector;
import com.ruoyi.qt.domain.QtAccessMetric;
import com.ruoyi.qt.mapper.QtAccessMetricMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QtAccessMetricServiceImplTest
{
    @Spy
    private AccessMetricsCollector collector = new AccessMetricsCollector();

    @Mock
    private QtAccessMetricMapper metricMapper;

    @InjectMocks
    private QtAccessMetricServiceImpl service;

    @BeforeEach
    void setFields()
    {
        ReflectionTestUtils.setField(service, "topUris", 20);
        ReflectionTestUtils.setField(service, "retainDays", 30);
    }

    @Test
    void flushPersistsGlobalAndTopUris()
    {
        collector.record("/qt/book/list", "GET", 200, 12);
        collector.record("/login", "POST", 401, 30);
        service.flush();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QtAccessMetric>> captor = ArgumentCaptor.forClass(List.class);
        verify(metricMapper).insertBatch(captor.capture());
        List<QtAccessMetric> rows = captor.getValue();
        assertTrue(rows.size() >= 2);
        assertEquals("*", rows.get(0).getUri());
        assertEquals(2, rows.get(0).getRequestCount().intValue());
        assertTrue(rows.stream().anyMatch(row -> "/qt/book/list".equals(row.getUri())));
    }
}
