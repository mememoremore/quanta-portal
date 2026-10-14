package com.ruoyi.qt.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.qt.service.IQtAccessMetricService;

@Component
@ConditionalOnProperty(prefix = "qt.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QtAccessMetricJob
{
    @Autowired
    private IQtAccessMetricService qtAccessMetricService;

    @Scheduled(initialDelayString = "${qt.metrics.window-seconds:60}000",
            fixedRateString = "${qt.metrics.window-seconds:60}000")
    public void flush()
    {
        qtAccessMetricService.flush();
    }

    @Scheduled(cron = "0 15 3 * * ?")
    public void cleanExpired()
    {
        qtAccessMetricService.cleanExpired();
    }
}
